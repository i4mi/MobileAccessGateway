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

package ch.bfh.ti.i4mi.mag.mhd.iti67;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntryType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindMedicationListQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

/**
 * ITI-67 to PHARM-1 request converter
 * 
 * @author Oliver Egger
 *
 */
public class Pharm1RequestConverter extends BaseRequestConverter {

    /**
     * convert ITI-67 request to CMPD Pharm-1
     * 
     * @param searchParameter
     * @return
     */
    public QueryRegistry searchParameterIti67ToFindMedicationListQuery(@Body Iti67SearchParameters searchParameter) {

        boolean getLeafClass = true;

        FindMedicationListQuery query = new FindMedicationListQuery();

        // status --> $XDSDocumentEntryStatus
        TokenOrListParam status = searchParameter.getStatus();
        if (status != null) {
            List<AvailabilityStatus> availabilites = new ArrayList<AvailabilityStatus>();
            for (TokenParam statusToken : status.getValuesAsQueryTokens()) {
                String tokenValue = statusToken.getValue();
                if (tokenValue.equals("current"))
                    availabilites.add(AvailabilityStatus.APPROVED);
                else if (tokenValue.equals("superseded"))
                    availabilites.add(AvailabilityStatus.DEPRECATED);
            }
            query.setStatus(availabilites);
        }

        // patient or patient.identifier --> $XDSDocumentEntryPatientId
        TokenParam tokenIdentifier = searchParameter.getPatientIdentifier();
        if (tokenIdentifier != null) {
            String system = getScheme(tokenIdentifier.getSystem());
            if (system == null)
                throw new InvalidRequestException("Missing OID for patient");
            query.setPatientId(new Identifiable(tokenIdentifier.getValue(), new AssigningAuthority(system)));
        }
        ReferenceParam patientRef = searchParameter.getPatientReference();
        if (patientRef != null) {
            Identifiable id = transformReference(patientRef.getValue());
            query.setPatientId(id);
        }

        // format --> $XDSDocumentEntryFormatCode
        TokenOrListParam formats = searchParameter.getFormat();
        query.setFormatCodes(codesFromTokens(formats));

        // add on-demand documents also to the query, maybe make it configurable
        if (true) {
            List<DocumentEntryType> documentEntryTypes = new ArrayList<DocumentEntryType>();
            documentEntryTypes.add(DocumentEntryType.ON_DEMAND);
            documentEntryTypes.add(DocumentEntryType.STABLE);
            query.setDocumentEntryTypes(documentEntryTypes);
        }

        final QueryRegistry queryRegistry = new QueryRegistry(query);
        queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);

        return queryRegistry;
    }
}
