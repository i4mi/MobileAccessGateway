package ch.bfh.ti.i4mi.mag.ppqm;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.support.DefaultExchange;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.platform.camel.core.util.Exchanges;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmConsentCreator.create201Consent;
import static org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmConsentCreator.createUuid;

/**
 * @author Dmytro Rud
 */
public class Ppq3Test extends PpqmTestBase {

    private Exchange send(Object request, String httpMethod) throws Exception {
        Exchange exchange = new DefaultExchange(camelContext, ExchangePattern.InOut);
        exchange.getMessage().setBody(request);
        exchange.getMessage().setHeader(Constants.HTTP_METHOD, httpMethod);
        exchange = producerTemplate.send("ch-ppq3://localhost:" + serverPort + "/fhir?secure=false" , exchange);
        Exception exception = Exchanges.extractException(exchange);
        if (exception != null) {
            throw exception;
        }
        return exchange;
    }

    @Test
    public void testPost() throws Exception {
        Consent consent = create201Consent(createUuid(), TestConstants.EPR_SPID);
        Exchange exchange = send(consent, "POST");
        MethodOutcome methodOutcome = exchange.getMessage().getMandatoryBody(MethodOutcome.class);
        assertTrue(methodOutcome.getCreated());
    }

    @Test
    public void testPostFailure() throws Exception {
        Consent consent = create201Consent(TestConstants.FAILURE_POLICY_SET_ID, TestConstants.EPR_SPID);
        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(consent, "POST");
        } catch (BaseServerResponseException e) {
            assertEquals(400, e.getStatusCode());
            OperationOutcome operationOutcome = (OperationOutcome) e.getOperationOutcome();
            assertEquals(1, operationOutcome.getIssue().size());
            exceptionHandled = true;
        }
        assertTrue(exceptionHandled);
    }

    @Test
    public void testPutUnknown() throws Exception {
        Consent consent = create201Consent(createUuid(), TestConstants.EPR_SPID);
        Exchange exchange = send(consent, "PUT");
        MethodOutcome methodOutcome = exchange.getMessage().getMandatoryBody(MethodOutcome.class);
        assertTrue(methodOutcome.getCreated());
    }

    @Test
    public void testPutKnown() throws Exception {
        Consent consent = create201Consent(TestConstants.KNOWN_POLICY_SET_ID, TestConstants.EPR_SPID);
        Exchange exchange = send(consent, "PUT");
        MethodOutcome methodOutcome = exchange.getMessage().getMandatoryBody(MethodOutcome.class);
        assertTrue((methodOutcome.getCreated() == null) || !methodOutcome.getCreated());
    }

    @Test
    public void testDeleteKnown() throws Exception {
        Exchange exchange = send(TestConstants.KNOWN_POLICY_SET_ID, "DELETE");
        MethodOutcome methodOutcome = exchange.getMessage().getMandatoryBody(MethodOutcome.class);
    }

    @Test
    public void testDeleteUnknown() throws Exception {
        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(createUuid(), "DELETE");
        } catch (BaseServerResponseException e) {
            assertEquals(404, e.getStatusCode());
            OperationOutcome operationOutcome = (OperationOutcome) e.getOperationOutcome();
            assertEquals(1, operationOutcome.getIssue().size());
            assertTrue(operationOutcome.getIssue().get(0).getDiagnostics().startsWith("Unknown policy set urn:uuid:"));
            exceptionHandled = true;
        }
        assertTrue(exceptionHandled);
    }

    @Test
    public void testDeleteFailure() throws Exception {
        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(TestConstants.FAILURE_POLICY_SET_ID, "DELETE");
        } catch (BaseServerResponseException e) {
            assertEquals(400, e.getStatusCode());
            OperationOutcome operationOutcome = (OperationOutcome) e.getOperationOutcome();
            assertEquals(1, operationOutcome.getIssue().size());
            assertTrue(operationOutcome.getIssue().get(0).getDiagnostics().startsWith("<ns1:Fault"));
            exceptionHandled = true;
        }
        assertTrue(exceptionHandled);
    }

}
