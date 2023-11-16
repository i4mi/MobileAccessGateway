package ch.bfh.ti.i4mi.mag.ppqm;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.support.DefaultExchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Consent;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xacml20.model.PpqConstants;
import org.openehealth.ipf.platform.camel.core.util.Exchanges;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmConsentCreator.createUuid;

/**
 * @author Dmytro Rud
 */
public class Ppq5Test extends PpqmTestBase {

    private Exchange send(ICriterion<?> criterion) throws Exception {
        Exchange exchange = new DefaultExchange(camelContext, ExchangePattern.InOut);
        exchange.getMessage().setBody(new ICriterion[]{criterion});
        exchange = producerTemplate.send("ch-ppq5://localhost:" + serverPort + "/fhir", exchange);
        Exception exception = Exchanges.extractException(exchange);
        if (exception != null) {
            throw exception;
        }
        return exchange;
    }

    @Test
    public void test1() throws Exception {
        ICriterion<TokenClientParam> criterion = Consent.IDENTIFIER.exactly().identifier(createUuid());
        Exchange exchange = send(criterion);
        Bundle bundle = exchange.getMessage().getMandatoryBody(Bundle.class);
        assertEquals(Bundle.BundleType.SEARCHSET, bundle.getType());
        assertEquals(0, bundle.getEntry().size());
    }

    @Test
    public void test2() throws Exception {
        ICriterion<TokenClientParam> criterion = Consent.IDENTIFIER.exactly().identifier(TestConstants.KNOWN_POLICY_SET_ID);
        Exchange exchange = send(criterion);
        Bundle bundle = exchange.getMessage().getMandatoryBody(Bundle.class);
        assertEquals(Bundle.BundleType.SEARCHSET, bundle.getType());
        assertEquals(1, bundle.getEntry().size());
    }

    @Test
    public void test3() throws Exception {
        ICriterion<TokenClientParam> criterion = new TokenClientParam("patient:identifier").exactly()
                .systemAndIdentifier(PpqConstants.CodingSystemIds.SWISS_PATIENT_ID, TestConstants.EPR_SPID);
        Exchange exchange = send(criterion);
        Bundle bundle = exchange.getMessage().getMandatoryBody(Bundle.class);
        assertEquals(Bundle.BundleType.SEARCHSET, bundle.getType());
        assertEquals(3, bundle.getEntry().size());
    }

}
