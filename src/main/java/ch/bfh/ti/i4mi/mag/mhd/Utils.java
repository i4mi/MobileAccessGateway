package ch.bfh.ti.i4mi.mag.mhd;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpMessage;
import org.hl7.fhir.r4.model.Patient;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.fhir.FhirSearchParameters;
import org.openehealth.ipf.commons.ihe.fhir.iti66.Iti66SearchParameters;
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType.DocumentResponse;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsByReferenceIdQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindSubmissionSetsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryList;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.Config;

/**
 * utility classes for MobileAccessGateway
 * @author alexander kreutz
 *
 */
public class Utils {

	public static final String KEPT_BODY = "KeptBody"; 
	
    public static FhirSearchParameters searchParameterToBody(@Headers Map<String, Object> parameters) {        
            FhirSearchParameters searchParameter = (FhirSearchParameters) parameters
                    .get(Constants.FHIR_REQUEST_PARAMETERS);
            return searchParameter;        
    }
    
    /**
     * keep current message body
     * @return
     */
    public static Processor keepBody() {
        return exchange -> {
        	exchange.setProperty(KEPT_BODY, exchange.getIn().getBody());        	        
        };
    }
    
    public static Processor endHttpSession() {
    	return exchange -> {
    		exchange.getIn(HttpMessage.class).getRequest().getSession().invalidate();
    	};
    }
    
    /**
     * move previously stored message body to "KeptBody" property
     * @return
     */
    public static Processor keptBodyToHeader() {
        return exchange -> {
        	exchange.getMessage().setHeader(KEPT_BODY, exchange.getProperty(KEPT_BODY));        	        
        };
    }
    
    
    
    public static Processor storePreferHeader() {
    	return exchange -> {
    
	    	Map<String, List<String>> httpHeaders = (Map<String, List<String>>) exchange.getMessage().getHeader("FhirHttpHeaders");
					
			if (httpHeaders != null) {
				List<String> header = httpHeaders.get("Prefer");
				if (header != null && !header.isEmpty()) {
					exchange.getMessage().setHeader("Prefer", header.get(0));
				}
			}
		};
    }

   

    /*
    public static Processor retrievedDocumentSetToHttResponse() {
        return exchange -> {
            RetrieveDocumentSetResponseType retrieveDocumentSetResponseType = (RetrieveDocumentSetResponseType) exchange.getIn().getBody();
            if ("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success".equals(retrieveDocumentSetResponseType.getRegistryResponse().getStatus())) {
                List<DocumentResponse> documentResponses = retrieveDocumentSetResponseType.getDocumentResponse();
                if (documentResponses.size()==1) {
                    DocumentResponse documentResponse = documentResponses.get(0);
                    final InputStream in = documentResponse.getDocument().getInputStream();
                    byte[] byteArray=org.apache.commons.io.IOUtils.toByteArray(in);
                    exchange.getIn().removeHeaders(".*");
                    exchange.getIn().setHeader("Content-Type", documentResponse.getMimeType());
                    exchange.getIn().setBody(byteArray);
                }
            } else {
               
            }
        };
    }
   */
}
