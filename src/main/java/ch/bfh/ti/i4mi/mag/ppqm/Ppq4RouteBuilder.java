package ch.bfh.ti.i4mi.mag.ppqm;

import ch.bfh.ti.i4mi.mag.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmUtils;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.XacmlToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.ChPpqMessageCreator;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.AssertionBasedRequestType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.EprPolicyRepositoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dmytro Rud
 */
@Component
@ConditionalOnProperty({"mag.ppq.ppq-1.url", "mag.ppq.ppq-2.url"})
@Slf4j
public class Ppq4RouteBuilder extends PpqmFeedRouteBuilder {

    @Getter
    private final String uriSchema = "ch-ppq4";

    @Autowired
    public Ppq4RouteBuilder(
            Config config,
            FhirToXacmlTranslator fhirToXacmlTranslator,
            ChPpqMessageCreator ppqMessageCreator)
    {
        super(config, fhirToXacmlTranslator, ppqMessageCreator);
    }

    @Override
    protected String extractHttpMethod(Exchange exchange) throws Exception {
        Bundle bundle = exchange.getMessage().getMandatoryBody(Bundle.class);
        return bundle.getEntry().get(0).getRequest().getMethod().toCode();
    }

    @Override
    protected List<String> extractPolicySetIds(Object ppqmRequest) {
        Bundle bundle = (Bundle) ppqmRequest;
        return bundle.getEntry().stream()
                .map(entry -> {
                    Consent consent = (Consent) entry.getResource();
                    return ChPpqmUtils.extractConsentId(consent, ChPpqmUtils.ConsentIdTypes.POLICY_SET_ID);
                })
                .collect(Collectors.toList());
    }

    @Override
    protected void setHttpMethodPost(Object ppqmRequest) throws Exception {
        Bundle bundle = (Bundle) ppqmRequest;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            entry.getRequest().setMethod(Bundle.HTTPVerb.POST);
            String url = entry.getRequest().getUrl();
            entry.getRequest().setUrl(url.substring(0, url.indexOf('?')));
        }
    }

    @Override
    protected AssertionBasedRequestType createPpqRequest(Object ppqmRequest, String method) {
        return fhirToXacmlTranslator.translatePpq4To1Request((Bundle) ppqmRequest);
    }

    @Override
    protected Object createPpqmResponse(Object ppqmRequest, AssertionBasedRequestType xacmlRequest, EprPolicyRepositoryResponse xacmlResponse) {
        return XacmlToFhirTranslator.translatePpq1To4Response((Bundle) ppqmRequest, xacmlRequest, xacmlResponse);
    }

}
