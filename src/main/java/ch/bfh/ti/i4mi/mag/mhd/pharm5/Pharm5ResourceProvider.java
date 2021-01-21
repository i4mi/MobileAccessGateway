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

package ch.bfh.ti.i4mi.mag.mhd.pharm5;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.fhir.AbstractPlainProvider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;

/**
 * This operation is the correspondence to the IHE CMPD PHARM-1 transaction but as an MHD extensios instead of XDS
 *
 * @author Oliver Egger
 */
public class Pharm5ResourceProvider extends AbstractPlainProvider {

    /**
     * Handles the PHARM5 Transactions
     *
     * @return {@link Parameters} containing found identifiers
     */
    @SuppressWarnings("unused")
    @Operation(name = Pharm5Constants.PHARM5_OPERATION_NAME, type = DocumentReference.class, idempotent = true, returnParameters = {@OperationParam(name = "return", type = Bundle.class, max = 100)})
    public IBundleProvider findMedicationList(
            @IdParam(optional = true) IdType resourceId,
            @OperationParam(name = Pharm5Constants.PHARM5_PATIENT_IDENTIFIER) TokenParam patientIdentifier,
            @OperationParam(name = Pharm5Constants.PHARM5_STATUS) StringParam status,
            @OperationParam(name = Pharm5Constants.PHARM5_FORMAT) TokenParam format,
            RequestDetails requestDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

    	// FIXME format should be TokenAndListParam
        var sourceIdentifier = new Identifier();

        if (resourceId == null) {
            sourceIdentifier.setSystem(patientIdentifier.getSystem())
                    .setValue(patientIdentifier.getValue());
        } else {
            sourceIdentifier.setValue(resourceId.getIdPart());
        }
        var statusType = status == null ? null : new StringType(status.getValue());

        
        Coding formatCoding = null;
        if (format!=null) {
            formatCoding = new Coding();
            formatCoding.setSystem(format.getSystem()).setCode(format.getValue());
        }

        var inParams = new Parameters();
        inParams.addParameter().setName(Pharm5Constants.PHARM5_PATIENT_IDENTIFIER).setValue(sourceIdentifier);
        inParams.addParameter().setName(Pharm5Constants.PHARM5_STATUS).setValue(statusType);
        if (formatCoding!=null) {
          inParams.addParameter().setName(Pharm5Constants.PHARM5_FORMAT).setValue(formatCoding);
        }
        
        return requestBundleProvider(inParams, null, ResourceType.DocumentReference.name(),
                httpServletRequest, httpServletResponse, requestDetails);
    }
}
