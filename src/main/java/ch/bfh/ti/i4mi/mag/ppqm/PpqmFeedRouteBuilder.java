package ch.bfh.ti.i4mi.mag.ppqm;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.ChPpqMessageCreator;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.AssertionBasedRequestType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.EprPolicyRepositoryResponse;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.assertion.AssertionType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.protocol.ResponseType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.assertion.XACMLPolicyStatementType;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelValidators;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Dmytro Rud
 */
@Slf4j
abstract public class PpqmFeedRouteBuilder extends PpqmRouteBuilder {

    public static final String PROP_FHIR_REQUEST  = "ppqm-fhir-request";
    public static final String PROP_FHIR_METHOD   = "ppqm-fhir-method";
    public static final String PROP_XACML_REQUEST = "ppqm-xacml-request";
    public static final String PROP_POLICY_COUNT  = "ppqm-policy-count";

    @Autowired
    protected PpqmFeedRouteBuilder(
            Config config,
            FhirToXacmlTranslator fhirToXacmlTranslator,
            ChPpqMessageCreator ppqMessageCreator)
    {
        super(config, fhirToXacmlTranslator, ppqMessageCreator);
    }

    /**
     * @return static value -- CH:PPQm Camel component schema served by this route builder.
     */
    abstract protected String getUriSchema();

    /**
     * @param exchange Camel exchange containing a request.
     * @return HTTP method specified of the request.
     */
    abstract protected String extractHttpMethod(Exchange exchange) throws Exception;

    /**
     * @param ppqmRequest CH:PPQm request message.
     * @return policy set IDs contained in the request message.
     */
    abstract protected List<String> extractPolicySetIds(Object ppqmRequest);

    /**
     * Changes HTTP method in the <b>body</b> of the CH:PPQm request message (whenever applicable) to POST.
     * @param ppqmRequest CH:PPQm request message.
     */
    abstract protected void setHttpMethodPost(Object ppqmRequest) throws Exception;

    /**
     * @param ppqmRequest CH:PPQm request message.
     * @param method HTTP method of the request message.
     * @return CH:PPQ request message (translation of the CH:PPQm one).
     */
    abstract protected AssertionBasedRequestType createPpqRequest(Object ppqmRequest, String method);

    /**
     * @param ppqmRequest CH:PPQm request message.
     * @param xacmlRequest CH:PPQ request message (translation of the CH:PPQm one).
     * @param xacmlResponse CH:PPQ response message (from the backend).
     * @return CH:PPQm response message.
     */
    abstract protected Object createPpqmResponse(
            Object ppqmRequest,
            AssertionBasedRequestType xacmlRequest,
            EprPolicyRepositoryResponse xacmlResponse);

    @Override
    public void configure() throws Exception {

        configureExceptionHandling();

        from(getUriSchema() + ":stub")
                .setHeader(FhirCamelValidators.VALIDATION_MODE, constant(FhirCamelValidators.MODEL))
                .process(FhirCamelValidators.itiRequestValidator())
				.process(RequestHeadersForwarder.checkAuthorization(config.isChPpqm()))
                .process(RequestHeadersForwarder.forward())
                .process(exchange -> {
                    Object body = exchange.getMessage().getBody();
                    String method = extractHttpMethod(exchange);
                    log.info("Received {} request of type {} with method {}", getUriSchema(), body.getClass().getSimpleName(), method);
                    exchange.setProperty(PROP_FHIR_REQUEST, body);
                    exchange.setProperty(PROP_FHIR_METHOD, method);
                })
                .choice()
                    .when(exchangeProperty(PROP_FHIR_METHOD).isEqualTo("PUT"))
                        .to("direct:handle-put-" + getUriSchema())
                        .end()
                .process(exchange -> {
                    Object ppqRequest = createPpqRequest(
                            exchange.getProperty(PROP_FHIR_REQUEST),
                            exchange.getProperty(PROP_FHIR_METHOD, String.class));
                    exchange.getMessage().setBody(ppqRequest);
                    exchange.setProperty(PROP_XACML_REQUEST, ppqRequest);
                    log.info("Created PPQ-1 {}", ppqRequest.getClass().getSimpleName());
                })
                .to("ch-ppq1://" + config.getPpq1HostUrl())
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(exchange -> {
                    log.info("Received PPQ-1 response, convert it to PPQm");
                    exchange.getMessage().setBody(createPpqmResponse(
                            exchange.getProperty(PROP_FHIR_REQUEST),
                            exchange.getProperty(PROP_XACML_REQUEST, AssertionBasedRequestType.class),
                            exchange.getMessage().getMandatoryBody(EprPolicyRepositoryResponse.class)));
                })
                .setHeader(FhirCamelValidators.VALIDATION_MODE, constant(FhirCamelValidators.MODEL))
                .process(FhirCamelValidators.itiResponseValidator())
        ;


        from("direct:handle-put-" + getUriSchema())
                .process(exchange -> {
                    List<String> policySetIds = extractPolicySetIds(exchange.getProperty(PROP_FHIR_REQUEST));
                    exchange.setProperty(PROP_POLICY_COUNT, policySetIds.size());
                    exchange.getMessage().setBody(ppqMessageCreator.createPolicyQuery(policySetIds));
                    log.info("Created PPQ-2 request for {} policy set(s)", policySetIds.size());
                })
				.process(RequestHeadersForwarder.checkAuthorization(config.isChPpqm()))
                .process(RequestHeadersForwarder.forward())
                .to("ch-ppq2://" + config.getPpq2HostUrl())
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(exchange -> {
                    log.info("Received PPQ-2 response");
                    ResponseType ppq2Response = exchange.getMessage().getMandatoryBody(ResponseType.class);
                    int presentPolicyCount = extractPresentPolicyCount(ppq2Response);
                    int fedPolicyCount = exchange.getProperty(PROP_POLICY_COUNT, Integer.class);
                    if (presentPolicyCount == fedPolicyCount) {
                        log.info("All policy sets being fed already exist in the Policy Repository, keep HTTP method PUT");
                    } else if (presentPolicyCount == 0) {
                        log.info("None of the policy sets being fed exists in the Policy Repository, switch HTTP method from PUT to POST");
                        exchange.setProperty(PROP_FHIR_METHOD, "POST");
                        setHttpMethodPost(exchange.getProperty(PROP_FHIR_REQUEST));
                    } else {
                        throw new InvalidRequestException(String.format(
                                "Cannot create PPQ-1 request, because out of %d policy sets being fed with HTTP method PUT, " +
                                        "%d are already present in the Policy Repository, and %d are not",
                                fedPolicyCount, presentPolicyCount, fedPolicyCount - presentPolicyCount));
                    }
                })
        ;
    }

    private static int extractPresentPolicyCount(ResponseType ppq2Response) {
        if ("urn:oasis:names:tc:SAML:2.0:status:Success".equals(ppq2Response.getStatus().getStatusCode().getValue())) {
            AssertionType assertion = (AssertionType) ppq2Response.getAssertionOrEncryptedAssertion().get(0);
            XACMLPolicyStatementType statement = (XACMLPolicyStatementType) assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().get(0);
            return statement.getPolicyOrPolicySet().size();
        }
        log.info("PPQ-2 response is negative");
        return 0;
    }

}
