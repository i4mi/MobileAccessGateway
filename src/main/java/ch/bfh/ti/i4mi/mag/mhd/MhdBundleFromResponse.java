package ch.bfh.ti.i4mi.mag.mhd;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorCode;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorInfo;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Severity;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class MhdBundleFromResponse extends MhdFromResponse implements ToFhirTranslator<Response> { 

	@Override
	public Object translateToFhir(Response input, Map<String, Object> parameters) {
		
		if (input.getStatus().equals(Status.SUCCESS)) {
		
			Bundle responseBundle = new Bundle();		
			Bundle requestBundle = (Bundle) parameters.get(Utils.KEPT_BODY);
			
			for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
	            Bundle.BundleEntryResponseComponent response = new Bundle.BundleEntryResponseComponent()
	                    .setStatus("201 Created")
	                    .setLastModified(new Date())
	                    .setLocation(requestEntry.getResource().getClass().getSimpleName() + "/" + 4711);
	            responseBundle.addEntry()
	                    .setResponse(response);
	                    
	        }
					
			return responseBundle;
		
		} else {	
			processError(input);
			return null;
		}
	}

}
