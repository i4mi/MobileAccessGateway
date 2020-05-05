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

package ch.ahdis.ipf.mag.pmir;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.openehealth.ipf.commons.ihe.fhir.AbstractPlainProvider;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
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
public class Iti93ResourceProvider extends AbstractPlainProvider {
    
    private static final long serialVersionUID = -8350324564184569852L;

    /**
     * /$process-message
     */
    @Operation(name = Iti93Constants.PMIR_OPERATION_NAME, idempotent = false, returnParameters = {@OperationParam(name = "return", type = Bundle.class, max = 1)})
    public IBaseBundle processMessage(
        @OperationParam(name = "content", min = 1, max = 1) Bundle content,
        RequestDetails requestDetails,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {
        
        return this.requestResource(content, null, IBaseBundle.class, httpServletRequest, httpServletResponse, requestDetails);
    }
}
