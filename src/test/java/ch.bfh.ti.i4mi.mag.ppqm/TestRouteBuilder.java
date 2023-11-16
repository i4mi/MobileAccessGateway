package ch.bfh.ti.i4mi.mag.ppqm;

import ch.bfh.ti.i4mi.mag.Config;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.velocity.VelocityContext;
import org.herasaf.xacml.core.policy.impl.IdReferenceType;
import org.herasaf.xacml.core.policy.impl.PolicySetType;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.translation.FhirToXacmlTranslator;
import org.openehealth.ipf.commons.ihe.xacml20.ChPpqMessageCreator;
import org.openehealth.ipf.commons.ihe.xacml20.ChPpqPolicySetCreator;
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants;
import org.openehealth.ipf.commons.ihe.xacml20.stub.UnknownPolicySetIdFaultMessage;
import org.openehealth.ipf.commons.ihe.xacml20.stub.ehealthswiss.*;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.assertion.XACMLPolicyStatementType;
import org.openehealth.ipf.commons.ihe.xacml20.stub.xacml20.saml.protocol.XACMLPolicyQueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.cxf.interceptor.Fault.FAULT_CODE_CLIENT;
import static org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmConsentCreator.createUuid;
import static org.openehealth.ipf.platform.camel.ihe.xacml20.Xacml20CamelValidators.*;

/**
 * @author Dmytro Rud
 */
@Component
public class TestRouteBuilder extends PpqmRouteBuilder {

    @Autowired
    public TestRouteBuilder(
            Config config,
            FhirToXacmlTranslator fhirToXacmlTranslator,
            ChPpqMessageCreator ppqMessageCreator)
    {
        super(config, fhirToXacmlTranslator, ppqMessageCreator);
    }

    @Override
    public void configure() throws Exception {

        from("ch-ppq1:ppq1Endpoint")
                .process(chPpq1RequestValidator())
                .process(exchange -> {
                    log.info("Received PPQ-1 request");
                    AssertionBasedRequestType ppq1Request = exchange.getMessage().getMandatoryBody(AssertionBasedRequestType.class);

                    /*
                        Business logic:
                        - On delete request with a known policy set ID -- return positive response.
                        - On delete request with a failure policy set ID -- throw a generic SOAP Fault.
                        - On delete request with an unknown policy set ID -- throw an UnknownPolicySetIdFault.
                        - On add/update request with failure policy set ID -- return negative response.
                        - On add/update request with any other policy set ID -- return positive response.
                    */

                    if (ppq1Request instanceof DeletePolicyRequest) {
                        XACMLPolicySetIdReferenceStatementType statement = (XACMLPolicySetIdReferenceStatementType) ppq1Request.getAssertion().getStatementOrAuthnStatementOrAuthzDecisionStatement().get(0);
                        String policySetId = statement.getPolicySetIdReference().get(0).getValue();
                        if (TestConstants.KNOWN_POLICY_SET_ID.equals(policySetId)) {
                            EprPolicyRepositoryResponse ppq1Response = new EprPolicyRepositoryResponse();
                            ppq1Response.setStatus(PpqConstants.StatusCode.SUCCESS);
                            exchange.getMessage().setBody(ppq1Response);
                        } else if (TestConstants.FAILURE_POLICY_SET_ID.equals(policySetId)) {
                            throw new SoapFault("Bad request", FAULT_CODE_CLIENT);
                        } else {
                            UnknownPolicySetId unknownPolicySetId = new UnknownPolicySetId();
                            unknownPolicySetId.setMessage(policySetId);
                            throw new UnknownPolicySetIdFaultMessage("Unknown policy set ID", unknownPolicySetId);
                        }
                    } else {
                        XACMLPolicyStatementType statement = (XACMLPolicyStatementType) ppq1Request.getAssertion().getStatementOrAuthnStatementOrAuthzDecisionStatement().get(0);
                        PolicySetType policySet = (PolicySetType) statement.getPolicyOrPolicySet().get(0);
                        EprPolicyRepositoryResponse ppq1Response = new EprPolicyRepositoryResponse();
                        ppq1Response.setStatus(policySet.getPolicySetId().toString().equals(TestConstants.FAILURE_POLICY_SET_ID)
                                ? PpqConstants.StatusCode.FAILURE
                                : PpqConstants.StatusCode.SUCCESS);
                        exchange.getMessage().setBody(ppq1Response);
                    }
                })
                .process(chPpq1ResponseValidator())
        ;

        from("ch-ppq2:ppq2Endpoint")
                .process(chPpq2RequestValidator())
                .process(exchange -> {
                    log.info("Received PPQ-2 request");
                    XACMLPolicyQueryType ppq2Request = exchange.getMessage().getMandatoryBody(XACMLPolicyQueryType.class);

                    /*
                        Business logic:
                        - If the "known" policy set is requested by its ID --> return it.
                        - Otherwise, if policy sets are requested by their IDs --> return empty response.
                        - Otherwise, i.e. if requested by patient  ID --> return three random policy sets.
                    */

                    List<PolicySetType> policySets = new ArrayList<>();

                    JAXBElement<?> jaxbElement = ppq2Request.getRequestOrPolicySetIdReferenceOrPolicyIdReference().get(0);
                    if (jaxbElement.getValue() instanceof IdReferenceType) {
                        IdReferenceType idReference = (IdReferenceType) jaxbElement.getValue();
                        if (TestConstants.KNOWN_POLICY_SET_ID.equals(idReference.getValue())) {
                            policySets.add(ChPpqPolicySetCreator.createPolicySet("201", new VelocityContext(Map.of(
                                    "id", TestConstants.KNOWN_POLICY_SET_ID,
                                    "eprSpid", TestConstants.EPR_SPID,
                                    "representativeId", "representative123"))));
                        }
                    } else {
                        for (int i = 0; i < 3; ++i) {
                            policySets.add(ChPpqPolicySetCreator.createPolicySet("303", new VelocityContext(Map.of(
                                    "id", createUuid(),
                                    "eprSpid", TestConstants.EPR_SPID,
                                    "representativeId", UUID.randomUUID().toString()))));
                        }
                    }

                    exchange.getMessage().setBody(ppqMessageCreator.createPositivePolicyQueryResponse(policySets));
                })
                .process(chPpq2ResponseValidator())
        ;

    }

}
