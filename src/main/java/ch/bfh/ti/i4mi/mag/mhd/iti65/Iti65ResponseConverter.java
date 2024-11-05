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

package ch.bfh.ti.i4mi.mag.mhd.iti65;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.ProvideAndRegisterDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorCode;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorInfo;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Severity;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ExtrinsicObjectType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import ch.bfh.ti.i4mi.mag.mhd.Utils;

/**
 * ITI-65 from ITI-41 response converter
 * @author alexander kreutz
 *
 */
public class Iti65ResponseConverter extends BaseResponseConverter implements ToFhirTranslator<Response> { 

	private Config config;
	
	public Iti65ResponseConverter(final Config config) {
		this.config = config;
	}
	
	/**
	 * convert ITI-41 response to ITI-65 response 
	 */
	@Override
	public Object translateToFhir(Response input, Map<String, Object> parameters) {

		String entryUuid = null;
		if (input.getStatus().equals(Status.SUCCESS)) {
			Bundle responseBundle = new Bundle();		
			ProvideAndRegisterDocumentSet prb = (ProvideAndRegisterDocumentSet) parameters.get("ProvideAndRegisterDocumentSet");
			entryUuid = Iti65RequestConverter.noPrefix(prb.getDocuments().get(0).getDocumentEntry().getEntryUuid());
			Bundle requestBundle = (Bundle) parameters.get("BundleRequest");						
			for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
	            Bundle.BundleEntryResponseComponent response = new Bundle.BundleEntryResponseComponent()
	                    .setStatus("201 Created")
	                    .setLastModified(new Date());
	            if (requestEntry.getResource() instanceof Binary) {
	              String uniqueId = (String) requestEntry.getResource().getUserData("masterIdentifier");
	              response.setLocation(config.getUriMagXdsRetrieve() + "?uniqueId=" + uniqueId
                     + "&repositoryUniqueId=" + config.getRepositoryUniqueId());	            
	            } else if (requestEntry.getResource() instanceof ListResource) {
	            	response.setLocation(config.getBaseurl()+"/fhir/List/"+Iti65RequestConverter.noPrefix(prb.getSubmissionSet().getEntryUuid()));
	            } else if (requestEntry.getResource() instanceof DocumentReference) {
	            	response.setLocation(config.getBaseurl()+"/fhir/DocumentReference/"+entryUuid);	              
	            }
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
