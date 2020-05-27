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

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.Config;

public class Utils {

    public static FhirSearchParameters searchParameterToBody(@Headers Map<String, Object> parameters) {        
            FhirSearchParameters searchParameter = (FhirSearchParameters) parameters
                    .get(Constants.FHIR_REQUEST_PARAMETERS);
            return searchParameter;        
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

    public static Timestamp timestampFromDateParam(DateParam dateParam) {
    	if (dateParam == null) return null;    	
    	String dateString = dateParam.getValueAsString();
    	dateString = dateString.replaceAll("-","");
    	return Timestamp.fromHL7(dateString);
    }
    
    public static Code codeFromToken(TokenParam param) {
    	return new Code(param.getValue(), null, param.getSystem());
    }
    
    public static List<Code> codesFromTokens(TokenOrListParam params) {
    	if (params == null) return null;
    	List<Code> codes = new ArrayList<Code>();
    	for (TokenParam token : params.getValuesAsQueryTokens()) {
    		codes.add(codeFromToken(token));
    	}
    	return codes;    	
    }
    
    public static QueryRegistry searchParameterIti67ToFindDocumentsQuery(@Body Iti67SearchParameters searchParameter) {
      
            boolean getLeafClass = true;
        
            FindDocumentsQuery query;
            
            //  TODO   related Note 4                  -->  $XDSDocumentEntryReferenceIdList
            TokenOrListParam related = searchParameter.getRelated();
            if (related != null) {
            	FindDocumentsByReferenceIdQuery referenceIdQuery = new FindDocumentsByReferenceIdQuery();;
            	QueryList<ReferenceId> outerReferences = new QueryList<ReferenceId>();
            	List<ReferenceId> references = new ArrayList<ReferenceId>();
            	for (TokenParam token : related.getValuesAsQueryTokens()) {
            		references.add(new ReferenceId(token.getValue(), null, token.getSystem()));
            	}
            	outerReferences.getOuterList().add(references);
            	referenceIdQuery.setTypedReferenceIds(outerReferences);
            	query = referenceIdQuery;            	
            } else query = new FindDocumentsQuery();          

            ReferenceParam patientRef = searchParameter.getPatientReference();           
            


            
//  (Not supported) Note 3          -->  $XDSDocumentEntryType

//            query.setMetadataLevel(metadataLevel);
            
            // status  -->  $XDSDocumentEntryStatus
            TokenOrListParam status = searchParameter.getStatus();
            if (status != null) {
	            List<AvailabilityStatus> availabilites = new ArrayList<AvailabilityStatus>();
	            for (TokenParam statusToken : status.getValuesAsQueryTokens()) {
	            	String tokenValue = statusToken.getValue();
	            	if (tokenValue.equals("current")) availabilites.add(AvailabilityStatus.APPROVED);
	            	else if (tokenValue.equals("superseded")) availabilites.add(AvailabilityStatus.DEPRECATED);
	            }            
	            query.setStatus(availabilites);
            }

            // patient or patient.identifier  -->  $XDSDocumentEntryPatientId
            TokenParam tokenIdentifier = searchParameter.getPatientIdentifier();
            if (tokenIdentifier != null) {
                String system = tokenIdentifier.getSystem();
                if (system.startsWith("urn:oid:")) {
                    system = system.substring(8);

                }
                query.setPatientId(new Identifiable(tokenIdentifier.getValue(), new AssigningAuthority(system)));
            }

            // date Note 1 Note 5              -->  $DSDocumentEntryCreationTimeFrom
            // date Note 2 Note 5              -->  $XDSDocumentEntryCreationTimeTo
            DateRangeParam dateRange = searchParameter.getDate();
            if (dateRange != null) {
	            DateParam creationTimeFrom = dateRange.getLowerBound();
	            DateParam creationTimeTo = dateRange.getUpperBound();
	            query.getCreationTime().setFrom(timestampFromDateParam(creationTimeFrom));
	            query.getCreationTime().setTo(timestampFromDateParam(creationTimeTo));
            }
           
            // period Note 1                   -->  $XDSDocumentEntryServiceStartTimeFrom
            // period Note 2                   -->  $XDSDocumentEntryServiceStartTimeTo
            // period Note 1                   -->  $XDSDocumentEntryServiceStopTimeFrom
            // period Note 2                   -->  $XDSDocumentEntryServiceStopTimeTo           
            DateRangeParam periodRange = searchParameter.getPeriod();
            if (periodRange != null) {
            	DateParam periodFrom = periodRange.getLowerBound();
            	DateParam periodTo = periodRange.getUpperBound();
            	            
            	query.getServiceStopTime().setFrom(timestampFromDateParam(periodFrom));
            	query.getServiceStartTime().setTo(timestampFromDateParam(periodTo));
            }
            
            //  category                        -->  $XDSDocumentEntryClassCode
            TokenOrListParam categories = searchParameter.getCategory();
            query.setClassCodes(codesFromTokens(categories));
                        
            //  type                            -->  $XDSDocumentEntryTypeCode
            TokenOrListParam types = searchParameter.getType();
            query.setTypeCodes(codesFromTokens(types));

            //  setting                         -->  $XDSDocumentEntryPracticeSettingCode
            TokenOrListParam settings = searchParameter.getSetting();
            query.setPracticeSettingCodes(codesFromTokens(settings));

            // facility                        -->  $XDSDocumentEntryHealthcareFacilityTypeCode
            TokenOrListParam facilities = searchParameter.getFacility();
            query.setHealthcareFacilityTypeCodes(codesFromTokens(facilities));

            // event                           -->  $XDSDocumentEntryEventCodeList
            TokenOrListParam events = searchParameter.getEvent();
            if (events != null) {
	            QueryList<Code> eventCodeList = new QueryList<Code>();
	            eventCodeList.getOuterList().add(codesFromTokens(events));
	            query.setEventCodes(eventCodeList);
            }
            
            //  security-label                  -->  $XDSDocumentEntryConfidentialityCode
            TokenOrListParam securityLabels = searchParameter.getSecurityLabel();
            if (securityLabels != null) {
            	QueryList<Code> confidentialityCodes = new QueryList<Code>();
            	confidentialityCodes.getOuterList().add(codesFromTokens(securityLabels));
            	query.setConfidentialityCodes(confidentialityCodes);
            }
            
            //  format                          -->  $XDSDocumentEntryFormatCode
            TokenOrListParam formats = searchParameter.getFormat();
            query.setFormatCodes(codesFromTokens(formats));
           
        
        //  TODO   author.given / author.family    -->  $XDSDocumentEntryAuthorPerson
            StringParam authorGivenName = searchParameter.getAuthorGivenName();
            StringParam authorFamilyName = searchParameter.getAuthorFamilyName();
            if (authorGivenName != null || authorFamilyName != null) {
	            String author = (authorGivenName != null ? authorGivenName.getValue() : "%")+" "+(authorFamilyName != null ? authorFamilyName.getValue() : "%");
	            List<String> authorPersons = Collections.singletonList(author);
	            query.setAuthorPersons(authorPersons);
            }

//            query.setDocumentAvailability(documentAvailabilities);

            final QueryRegistry queryRegistry = new QueryRegistry(query);
            queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);
            
            return queryRegistry;
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
