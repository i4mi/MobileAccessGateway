package ch.bfh.ti.i4mi.mag.mhd.iti67;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsByReferenceIdQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryList;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

public class Iti67RequestConverter extends BaseRequestConverter {


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
}
