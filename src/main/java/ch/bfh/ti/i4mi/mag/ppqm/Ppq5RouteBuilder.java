package ch.bfh.ti.i4mi.mag.ppqm;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Consent;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.XacmlToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.ChPpqMessageCreator;
import org.openehealth.ipf.commons.ihe.xacml20.stub.saml20.protocol.ResponseType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.protocol.XACMLPolicyQueryType;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelValidators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Dmytro Rud
 */
@Component
@ConditionalOnProperty("mag.ppq.ppq-2.url")
@Slf4j
public class Ppq5RouteBuilder extends PpqmRouteBuilder {

    @Autowired
    public Ppq5RouteBuilder(
            Config config,
            FhirToXacmlTranslator fhirToXacmlTranslator,
            ChPpqMessageCreator ppqMessageCreator)
    {
        super(config, fhirToXacmlTranslator, ppqMessageCreator);
    }

    @Override
    public void configure() throws Exception {

        configureExceptionHandling();

        from("ch-ppq5:stub")
                .setHeader(FhirCamelValidators.VALIDATION_MODE, constant(FhirCamelValidators.MODEL))
                .process(FhirCamelValidators.itiRequestValidator())
                .process(RequestHeadersForwarder.forward())
                .process(exchange -> {
                    String ppq5Request = exchange.getMessage().getHeader(Constants.HTTP_QUERY, String.class);
                    XACMLPolicyQueryType ppq2Request = fhirToXacmlTranslator.translatePpq5To2Request(ppq5Request);
                    exchange.getMessage().setBody(ppq2Request);
                    log.info("Received PPQ-5 request and converted to PPQ-2");
                })
                .to("ch-ppq2://" + config.getPpq2HostUrl())
                .process(exchange -> {
                    ResponseType ppq2Response = exchange.getMessage().getMandatoryBody(ResponseType.class);
                    List<Consent> ppq5Response = XacmlToFhirTranslator.translatePpq2To5Response(ppq2Response);
                    exchange.getMessage().setBody(ppq5Response);
                    log.info("Received PPQ-2 response and converted to PPQ-5");
                })
                .process(TraceparentHandler.updateHeaderForFhir())
                .setHeader(FhirCamelValidators.VALIDATION_MODE, constant(FhirCamelValidators.MODEL))
                .process(FhirCamelValidators.itiResponseValidator())
        ;

    }
}
