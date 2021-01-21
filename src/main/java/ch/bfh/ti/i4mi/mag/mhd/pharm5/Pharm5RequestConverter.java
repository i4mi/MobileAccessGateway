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

package ch.bfh.ti.i4mi.mag.mhd.pharm5;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Type;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntryType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindMedicationListQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

/**
 * ITI-67 to PHARM-1 request converter
 * 
 * @author Oliver Egger
 *
 */
public class Pharm5RequestConverter extends BaseRequestConverter {

    /**
     * convert PHARM-5 request to CMPD Pharm-1
     * 
     * @param searchParameter
     * @return
     */
    public QueryRegistry operationFindMedicationListToFindMedicationListQuery(@Body Parameters searchParameter) {

        boolean getLeafClass = true;

        FindMedicationListQuery query = new FindMedicationListQuery();

        // status --> $XDSDocumentEntryStatus
        List<Type> statusTypes = searchParameter.getParameters(Pharm5Constants.PHARM5_STATUS);
        if (statusTypes != null) {
            List<AvailabilityStatus> availabilites = new ArrayList<AvailabilityStatus>();
            for (Type status : statusTypes) {
                String tokenValue = status.primitiveValue();
                if (tokenValue.equals("current"))
                    availabilites.add(AvailabilityStatus.APPROVED);
                else if (tokenValue.equals("superseded"))
                    availabilites.add(AvailabilityStatus.DEPRECATED);
            }
            query.setStatus(availabilites);
        }

        // patient or patient.identifier --> $XDSDocumentEntryPatientId
        Type patientIdentifier = searchParameter.getParameter(Pharm5Constants.PHARM5_PATIENT_IDENTIFIER);
        if (patientIdentifier != null) {
            Identifier patIdentifier = (Identifier) patientIdentifier; 
            String system = patIdentifier.getSystem();
            if (system == null || !system.startsWith("urn:oid:"))
                throw new InvalidRequestException("Missing OID for patient");
            query.setPatientId(new Identifiable(patIdentifier.getValue(), new AssigningAuthority(system.substring(8))));
        }

        List<Type> formatTypes = searchParameter.getParameters(Pharm5Constants.PHARM5_FORMAT);
        if (formatTypes!=null && formatTypes.size()>0) {
            List<Code> formatCodes = new ArrayList<Code>();
            for (Type format : formatTypes) {
                Coding formatCoding = (Coding) format;
                Code formatCode = new Code();
                formatCode.setCode(formatCoding.getCode());
                String system = formatCoding.getSystem();
                if (system.startsWith("urn:oid:")) {
                    system = system.substring(8);
                }
                formatCode.setSchemeName(system);
                formatCodes.add(formatCode);
            }
            query.setFormatCodes(formatCodes);
        }

        List<DocumentEntryType> documentEntryTypes = new ArrayList<DocumentEntryType>();
        documentEntryTypes.add(DocumentEntryType.ON_DEMAND);
        documentEntryTypes.add(DocumentEntryType.STABLE);
        query.setDocumentEntryTypes(documentEntryTypes);

        final QueryRegistry queryRegistry = new QueryRegistry(query);
        queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);

        return queryRegistry;
    }
}
