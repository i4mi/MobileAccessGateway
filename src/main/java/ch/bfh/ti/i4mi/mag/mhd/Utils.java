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
import java.util.List;
import java.util.Map;

import org.apache.camel.Processor;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.fhir.FhirSearchParameters;
import org.openehealth.ipf.commons.ihe.fhir.iti66.Iti66SearchParameters;
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType.DocumentResponse;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindSubmissionSetsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.Config;

public class Utils {

    public static Processor searchParameterToBody() {
        return exchange -> {
            Map<String, Object> parameters = exchange.getIn().getHeaders();
            FhirSearchParameters searchParameter = (FhirSearchParameters) parameters
                    .get(Constants.FHIR_REQUEST_PARAMETERS);
            exchange.getIn().setBody(searchParameter);
        };
    }

    public static Processor searchParameterIti66ToFindSubmissionSetsQuery(Config config) {
        return exchange -> {

            boolean getLeafClass = true;

            Iti66SearchParameters searchParameter = (Iti66SearchParameters) exchange.getIn().getBody();

            final FindSubmissionSetsQuery query = new FindSubmissionSetsQuery();

            TokenParam tokenIdentifier = searchParameter.getPatientIdentifier();
            if (tokenIdentifier != null) {
//                query.setPatientId(new Identifiable(tokenIdentifier.getValue(), new AssigningAuthority(tokenIdentifier.getSystem())));
// FIXME PoC
                query.setPatientId(
                        new Identifiable(config.getPatientId(), new AssigningAuthority(config.getDomainMpiOid())));
            }

// TODO          query.setSourceIds(sourceIds);
// TODO         query.setStatus((statuses != null) ? statuses : new ArrayList<>());

            List<AvailabilityStatus> availabilites = new ArrayList<AvailabilityStatus>();
            availabilites.add(AvailabilityStatus.APPROVED);
            query.setStatus(availabilites);

// TODO         query.setContentTypeCodes(contentTypeCodes);
// TODO         query.setAuthorPerson(authorPerson);
// TODO         query.getSubmissionTime().setFrom(submissionTimeFrom);
// TODO           query.getSubmissionTime().setTo(submissionTimeTo);

            final QueryRegistry queryRegistry = new QueryRegistry(query);
            queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);

            exchange.getIn().setBody(queryRegistry);
        };
    }

    public static Processor searchParameterIti67ToFindDocumentsQuery(Config config) {
        return exchange -> {

            boolean getLeafClass = true;

            Iti67SearchParameters searchParameter = (Iti67SearchParameters) exchange.getIn().getBody();

            final FindDocumentsQuery query = new FindDocumentsQuery();

//  TODO   patient or patient.identifier   -->  $XDSDocumentEntryPatientId
//          query.setPatientId((patientId != null) ? patientId : new Identifiable());
//  TODO   date Note 1 Note 5              -->  $DSDocumentEntryCreationTimeFrom
//  TODO   date Note 2 Note 5              -->  $XDSDocumentEntryCreationTimeTo
//  TODO   author.given / author.family    -->  $XDSDocumentEntryAuthorPerson            
//  TODO   status                          -->  $XDSDocumentEntryStatus
//  TODO   (Not supported) Note 3          -->  $XDSDocumentEntryType
//  TODO   category                        -->  $XDSDocumentEntryClassCode
//  TODO   type                            -->  $XDSDocumentEntryTypeCode
//  TODO   setting                         -->  $XDSDocumentEntryPracticeSettingCode
//  TODO   period Note 1                   -->  $XDSDocumentEntryServiceStartTimeFrom
//  TODO   period Note 2                   -->  $XDSDocumentEntryServiceStartTimeTo
//  TODO   period Note 1                   -->  $XDSDocumentEntryServiceStopTimeFrom
//  TODO   period Note 2                   -->  $XDSDocumentEntryServiceStopTimeTo
//  TODO   facility                        -->  $XDSDocumentEntryHealthcareFacilityTypeCode
//  TODO   event                           -->  $XDSDocumentEntryEventCodeList
//  TODO   security-label                  -->  $XDSDocumentEntryConfidentialityCode
//  TODO   format                          -->  $XDSDocumentEntryFormatCode
//  TODO   related Note 4                  -->  $XDSDocumentEntryReferenceIdList

//            query.setMetadataLevel(metadataLevel);
            List<AvailabilityStatus> availabilites = new ArrayList<AvailabilityStatus>();
            availabilites.add(AvailabilityStatus.APPROVED);
            query.setStatus(availabilites);

            TokenParam tokenIdentifier = searchParameter.getPatientIdentifier();
            if (tokenIdentifier != null) {
                String system = tokenIdentifier.getSystem();
                if (system.startsWith("urn:oid:")) {
                    system = system.substring(8);

                }
                query.setPatientId(new Identifiable(tokenIdentifier.getValue(), new AssigningAuthority(system)));
            }

//            query.setClassCodes(classCodes);
//            query.setTypeCodes(typeCodes);
//            query.setPracticeSettingCodes(practiceSettingCodes);
//            query.setAuthorPersons(authorPersons);
//            query.getCreationTime().setFrom(creationTimeFrom);
//            query.getCreationTime().setTo(creationTimeTo);
//            query.getServiceStartTime().setFrom(serviceStartTimeFrom);
//            query.getServiceStartTime().setTo(serviceStartTimeTo);
//            query.getServiceStopTime().setFrom(serviceStopTimeFrom);
//            query.getServiceStopTime().setTo(serviceStopTimeTo);
//            query.setHealthcareFacilityTypeCodes(healthcareFacilityTypeCodes);
//            query.setEventCodes(eventCodeList);
//            query.setConfidentialityCodes(confidentialityCodes);
//            query.setFormatCodes(formatCodes);
//            query.setDocumentEntryTypes(types);
//            query.setDocumentAvailability(documentAvailabilities);

            final QueryRegistry queryRegistry = new QueryRegistry(query);
            queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);

            exchange.getIn().setBody(queryRegistry);
        };
    }

    public static Processor queryParameterToRetrieveDocumentSet() {
        return exchange -> {
            Map<String, Object> parameters = exchange.getIn().getHeaders();

            RetrieveDocumentSet retrieveDocumentSet = new RetrieveDocumentSet();
            DocumentEntry documentEntry = new DocumentEntry();
            if (parameters.containsKey("repositoryUniqueId")) {
                documentEntry.setRepositoryUniqueId(parameters.getOrDefault("repositoryUniqueId", "").toString());
            }
            if (parameters.containsKey("uniqueId")) {
                documentEntry.setUniqueId(parameters.getOrDefault("uniqueId", "").toString());
            }
//            documentEntry.setHomeCommunityId("");
            retrieveDocumentSet.addReferenceTo(documentEntry);
            
            exchange.getIn().setBody(retrieveDocumentSet);
        };
    }
    
    
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
                // throw error;
            }
        };
    }

}
