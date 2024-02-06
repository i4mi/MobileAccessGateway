package ch.bfh.ti.i4mi.mag.ppqm;

import ch.bfh.ti.i4mi.mag.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Consent;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmUtils;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.XacmlToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.ChPpqMessageCreator;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.AssertionBasedRequestType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.EprPolicyRepositoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author Dmytro Rud
 */
@Component
@ConditionalOnProperty({"mag.ppq.ppq-1.url", "mag.ppq.ppq-2.url"})
@Slf4j
public class Ppq3RouteBuilder extends PpqmFeedRouteBuilder {

    @Getter
    private final String uriSchema = "ch-ppq3";

    @Autowired
    public Ppq3RouteBuilder(
            Config config,
            FhirToXacmlTranslator fhirToXacmlTranslator,
            ChPpqMessageCreator ppqMessageCreator)
    {
        super(config, fhirToXacmlTranslator, ppqMessageCreator);
    }

    @Override
    protected String extractHttpMethod(Exchange exchange) {
        return exchange.getMessage().getHeader(Constants.HTTP_METHOD, String.class);
    }

    @Override
    protected List<String> extractPolicySetIds(Object ppqmRequest) {
        Consent consent = (Consent) ppqmRequest;
        String consentId = ChPpqmUtils.extractConsentId(consent, ChPpqmUtils.ConsentIdTypes.POLICY_SET_ID);
        return Collections.singletonList(consentId);
    }

    @Override
    protected void setHttpMethodPost(Object ppqmRequest) throws Exception {
        // nop, not applicable for CH:PPQ-3
    }

    @Override
    protected AssertionBasedRequestType createPpqRequest(Object ppqmRequest, String method) {
        return fhirToXacmlTranslator.translatePpq3To1Request(ppqmRequest, method);
    }

    @Override
    protected Object createPpqmResponse(Object ppqmRequest, AssertionBasedRequestType xacmlRequest, EprPolicyRepositoryResponse xacmlResponse) {
        return XacmlToFhirTranslator.translatePpq1To3Response(
                (ppqmRequest instanceof Consent) ? (Consent) ppqmRequest : null,
                xacmlRequest,
                xacmlResponse);
    }

}
