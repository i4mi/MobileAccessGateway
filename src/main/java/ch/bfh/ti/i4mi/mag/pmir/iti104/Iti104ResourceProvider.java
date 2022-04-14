/*
 * Copyright 2015 the original author or authors.
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

package ch.bfh.ti.i4mi.mag.pmir.iti104;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.openehealth.ipf.commons.ihe.fhir.AbstractPlainProvider;

import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;

/**
 * According to the PMIR specification, this resource provider must handle requests in the form
 * POST [base]/$process-message
 * 
 * This functionality should actually be moved into org.openehealth.ipf.commons.ihe.fhir.SharedFhirProvider since $process-message can be invoked for multiple actors 
 * equivalant to org.openehealth.ipf.commons.ihe.fhir.support.BatchTransactionResourceProvider
 *  
 * @author Oliver Egger
 */
public class Iti104ResourceProvider extends AbstractPlainProvider {
    
    private static final long serialVersionUID = -8350324564184569852L;
  
   
    /**
     * Handles Conditional update according to https://profiles.ihe.net/ITI/PIXm/ITI-104.html
     *
     * @param id resource ID
     * @param httpServletRequest servlet request
     * @param httpServletResponse servlet response
     * @param requestDetails      request details
     * @return {@link DocumentManifest} resource
     */
    @SuppressWarnings("unused")
    @Update()
    public MethodOutcome patientUpdate(
            @ResourceParam Patient thePatient,
            @IdParam IdType theId, 
            @ConditionalUrlParam String theConditional,
            RequestDetails requestDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
     
      return requestAction(thePatient, null, httpServletRequest, httpServletResponse, requestDetails);
    }
    
   
}
