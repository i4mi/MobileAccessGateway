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
import java.util.Collections;
import java.util.List;

import ca.uhn.fhir.rest.param.*;
import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntryType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.XcnName;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsByReferenceIdQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.Query;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryList;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

/**
 * ITI-67 to ITI-18 request converter
 * @author alexander kreutz
 *
 */
public class Iti67RequestConverter extends BaseRequestConverter {

	/**
	  * convert ITI-67 request to ITI-18 request
	  * @param searchParameter
	  * @return
	  */
    public QueryRegistry searchParameterIti67ToFindDocumentsQuery(@Body Iti67SearchParameters searchParameter) {
      
            boolean getLeafClass = true;
        
            Query searchQuery = null;
                                    
            if (searchParameter.get_id() != null || searchParameter.getIdentifier() != null) {
            	GetDocumentsQuery query = new GetDocumentsQuery();
            	if (searchParameter.getIdentifier() != null) {
	   	        	 String val = searchParameter.getIdentifier().getValue();
	   	        	 if (val.startsWith("urn:oid:")) {
	   	        		 query.setUniqueIds(Collections.singletonList(val.substring("urn:oid:".length())));
	   	        	 } else if (val.startsWith("urn:uuid:")) {
	   	        		 query.setUuids(Collections.singletonList(val.substring("urn:uuid:".length())));
	   	        	 }
           	    } else {
           		     query.setUuids(Collections.singletonList(searchParameter.get_id().getValue()));
           	    }            	            	
            	searchQuery = query;
            } else {
            	FindDocumentsQuery query;
	            //  TODO   related Note 4                  -->  $XDSDocumentEntryReferenceIdList
	            TokenOrListParam related = searchParameter.getRelatedId();
	            if (related != null) {
	            	FindDocumentsByReferenceIdQuery referenceIdQuery = new FindDocumentsByReferenceIdQuery();;
	            	QueryList<ReferenceId> outerReferences = new QueryList<ReferenceId>();
	            	List<ReferenceId> references = new ArrayList<ReferenceId>();
	            	for (TokenParam token : related.getValuesAsQueryTokens()) {
	            		references.add(new ReferenceId(token.getValue(), null, getScheme(token.getSystem())));
	            	}
	            	outerReferences.getOuterList().add(references);
	            	referenceIdQuery.setTypedReferenceIds(outerReferences);
	            	query = referenceIdQuery;            	
	            } else query = new FindDocumentsQuery();          
	                       
	
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
	                String system = getScheme(tokenIdentifier.getSystem());   
	                if (system==null) throw new InvalidRequestException("Missing OID for patient");
	                query.setPatientId(new Identifiable(tokenIdentifier.getValue(), new AssigningAuthority(system)));
	            }
	            ReferenceParam patientRef =  searchParameter.getPatientReference();
	            if (patientRef != null) {
	           	 Identifiable id = transformReference(patientRef.getValue());
	           	 query.setPatientId(id);
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
	            	Person person = new Person();
	            	
	            	XcnName name = new XcnName();
					if (authorFamilyName != null) name.setFamilyName(authorFamilyName.getValue());
					if (authorGivenName != null)  name.setGivenName(authorGivenName.getValue());
	            	person.setName(name);
		            //String author = (authorGivenName != null ? authorGivenName.getValue() : "%")+" "+(authorFamilyName != null ? authorFamilyName.getValue() : "%");
		            List<Person> authorPersons = Collections.singletonList(person);
		            query.setTypedAuthorPersons(authorPersons);
		            
	            }
	            searchQuery = query;
            }
            final QueryRegistry queryRegistry = new QueryRegistry(searchQuery);
            queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);
            
            return queryRegistry;
    }

}
