package ch.bfh.ti.i4mi.mag.ppqm;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.support.DefaultExchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmUtils;
import org.openehealth.ipf.platform.camel.core.util.Exchanges;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openehealth.ipf.commons.ihe.fhir.chppqm.ChPpqmConsentCreator.*;

/**
 * @author Dmytro Rud
 */
@Ignore
public class Ppq4Test extends PpqmTestBase {

    private Exchange send(Bundle request) throws Exception {
        Exchange exchange = new DefaultExchange(camelContext, ExchangePattern.InOut);
        exchange.getMessage().setBody(request);
        exchange = producerTemplate.send("ch-ppq4://localhost:" + serverPort + "/fhir", exchange);
        Exception exception = Exchanges.extractException(exchange);
        if (exception != null) {
            throw exception;
        }
        return exchange;
    }

    @Test
    public void testPost() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4SubmitRequestBundle(List.of(
                        create201Consent(createUuid(), TestConstants.EPR_SPID),
                        create303Consent(createUuid(), TestConstants.EPR_SPID, "rep123", null, null)),
                Bundle.HTTPVerb.POST);

        Exchange exchange = send(requestBundle);
        Bundle responseBundle = exchange.getMessage().getMandatoryBody(Bundle.class);

        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, responseBundle.getType());
        assertEquals(requestBundle.getEntry().size(), responseBundle.getEntry().size());
        for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
            assertTrue(entry.getResponse().getStatus().startsWith("201"));
        }
    }

    @Test
    public void testPostFailure() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4SubmitRequestBundle(List.of(
                        create201Consent(TestConstants.FAILURE_POLICY_SET_ID, TestConstants.EPR_SPID)),
                Bundle.HTTPVerb.POST);

        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(requestBundle);
        } catch (BaseServerResponseException e) {
            assertEquals(400, e.getStatusCode());
            OperationOutcome operationOutcome = (OperationOutcome) e.getOperationOutcome();
            assertEquals(1, operationOutcome.getIssue().size());
            exceptionHandled = true;
        }
        assertTrue(exceptionHandled);
    }

    @Test
    public void testPutAllUnknown() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4SubmitRequestBundle(List.of(
                        create201Consent(createUuid(), TestConstants.EPR_SPID),
                        create303Consent(createUuid(), TestConstants.EPR_SPID, "rep123", null, null)),
                Bundle.HTTPVerb.PUT);

        Exchange exchange = send(requestBundle);
        Bundle responseBundle = exchange.getMessage().getMandatoryBody(Bundle.class);

        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, responseBundle.getType());
        assertEquals(requestBundle.getEntry().size(), responseBundle.getEntry().size());
        for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
            assertTrue(entry.getResponse().getStatus().startsWith("201"));
        }
    }

    @Test
    public void testPutAllKnown() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4SubmitRequestBundle(List.of(
                        create201Consent(TestConstants.KNOWN_POLICY_SET_ID, TestConstants.EPR_SPID)),
                Bundle.HTTPVerb.PUT);

        Exchange exchange = send(requestBundle);
        Bundle responseBundle = exchange.getMessage().getMandatoryBody(Bundle.class);

        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, responseBundle.getType());
        assertEquals(requestBundle.getEntry().size(), responseBundle.getEntry().size());
        for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
            assertTrue(entry.getResponse().getStatus().startsWith("200"));
        }
    }

    @Test
    public void testPutMixedKnownAndUnknown() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4SubmitRequestBundle(List.of(
                        create201Consent(TestConstants.KNOWN_POLICY_SET_ID, TestConstants.EPR_SPID),
                        create303Consent(createUuid(), TestConstants.EPR_SPID, "rep123", null, null)),
                Bundle.HTTPVerb.PUT);

        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(requestBundle);
        } catch (Exception e) {
            assertTrue(e instanceof InvalidRequestException);
            assertTrue(e.getMessage().contains("Cannot create PPQ-1 request, because out of 2 policy sets being fed with HTTP method PUT, 1 are already present in the Policy Repository, and 1 are not"));
            exceptionHandled = true;
        }
        assertTrue(exceptionHandled);
    }

    @Test
    public void testDeleteKnown() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4DeleteRequestBundle(List.of(TestConstants.KNOWN_POLICY_SET_ID));

        Exchange exchange = send(requestBundle);
        Bundle responseBundle = exchange.getMessage().getMandatoryBody(Bundle.class);

        assertEquals(Bundle.BundleType.TRANSACTIONRESPONSE, responseBundle.getType());
        assertEquals(requestBundle.getEntry().size(), responseBundle.getEntry().size());
        for (Bundle.BundleEntryComponent entry : responseBundle.getEntry()) {
            assertTrue(entry.getResponse().getStatus().startsWith("200"));
        }
    }

    @Test
    public void testDeleteUnknown() throws Exception {
        Bundle requestBundle = ChPpqmUtils.createPpq4DeleteRequestBundle(List.of(createUuid()));

        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(requestBundle);
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
        Bundle requestBundle = ChPpqmUtils.createPpq4DeleteRequestBundle(List.of(TestConstants.FAILURE_POLICY_SET_ID));

        boolean exceptionHandled = false;
        try {
            Exchange exchange = send(requestBundle);
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
