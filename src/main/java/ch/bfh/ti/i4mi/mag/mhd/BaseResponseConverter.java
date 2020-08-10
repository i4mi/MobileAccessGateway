/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.bfh.ti.i4mi.mag.mhd;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorCode;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorInfo;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Severity;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

/**
 * base class for XDS to MHD response converters
 * @author alexander kreutz
 *
 */
public class BaseResponseConverter {

	/** 
	 * XDS error response -> FHIR error response
	 * @param input
	 */
	public void processError(Response input) {		
		throw new InvalidRequestException("Retrieved error response", processErrorAsOutcome(input));
	}
	
	/**
	 * XDS error response -> FHIR OperationOutcome
	 * @param input
	 * @return
	 */
	public OperationOutcome processErrorAsOutcome(Response input) {
		OperationOutcome outcome = new OperationOutcome();
		
		List<ErrorInfo> errors = input.getErrors();
		for (ErrorInfo info : errors) {
			OperationOutcomeIssueComponent issue = outcome.addIssue();
		    
		    Severity sevirity = info.getSeverity();
		    if (sevirity.equals(Severity.ERROR)) issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
		    else if (sevirity.equals(Severity.WARNING)) issue.setSeverity(OperationOutcome.IssueSeverity.WARNING);
		    
		    ErrorCode errorCode = info.getErrorCode();
		    issue.setCode(IssueType.INVALID);
		    // TODO map error codes
		    //if (errorCode.equals(ErrorCode.REGISTRY_ERROR)) issue.setCode(IssueType.STRUCTURE);
		    //else if (errorCode.equals(ErrorCode.REGISTRY_METADATA_ERROR)) issue.setCode(IssueType.STRUCTURE);
		    //else 
		    
		    issue.setDetails(new CodeableConcept().setText(info.getCodeContext()).addCoding(new Coding().setCode(errorCode.toString())));
		    
		    issue.setLocation(Collections.singletonList(new StringType(info.getLocation())));
		}
		
		return outcome;
	}
}
