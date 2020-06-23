package ch.bfh.ti.i4mi.mag.mhd.iti66;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.fhir.iti66.Iti66SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindSubmissionSetsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.mhd.BaseRequestConverter;

public class Iti66RequestConverter extends BaseRequestConverter {

	 public static QueryRegistry searchParameterIti66ToFindSubmissionSetsQuery(@Body Iti66SearchParameters searchParameter) {
	      
         boolean getLeafClass = true;
       
         final FindSubmissionSetsQuery query = new FindSubmissionSetsQuery();

         // patient or patient.identifier -> $XDSSubmissionSetPatientId
         TokenParam tokenIdentifier = searchParameter.getPatientIdentifier();
         if (tokenIdentifier != null) {
         	String system = tokenIdentifier.getSystem();
         	if (system.startsWith("urn:oid:")) {
                 system = system.substring(8);
             }
         	
              query.setPatientId(new Identifiable(tokenIdentifier.getValue(), new AssigningAuthority(system)));
         }
        
         // created Note 1 -> $XDSSubmissionSetSubmissionTimeFrom
         // created Note 2 -> $XDSSubmissionSetSubmissionTimeTo 
         DateRangeParam createdRange = searchParameter.getCreated();
         if (createdRange != null) {
	            DateParam creationTimeFrom = createdRange.getLowerBound();
	            DateParam creationTimeTo = createdRange.getUpperBound();
	            query.getSubmissionTime().setFrom(timestampFromDateParam(creationTimeFrom));
	            query.getSubmissionTime().setTo(timestampFromDateParam(creationTimeTo));
         }            
         
         // TODO author.given / author.family -> $XDSSubmissionSetAuthorPerson
         StringParam authorGivenName = searchParameter.getAuthorGivenName();
         StringParam authorFamilyName = searchParameter.getAuthorFamilyName();
         if (authorGivenName != null || authorFamilyName != null) {
	            String author = (authorGivenName != null ? authorGivenName.getValue() : "%")+" "+(authorFamilyName != null ? authorFamilyName.getValue() : "%");	            
	            query.setAuthorPerson(author);
         }
                                 
         // type -> $XDSSubmissionSetContentType
         TokenOrListParam types = searchParameter.getType();
         query.setContentTypeCodes(codesFromTokens(types));
         
         
         // source -> $XDSSubmissionSetSourceId 
         TokenOrListParam sources = searchParameter.getSource();
         query.setSourceIds(urisFromTokens(sources));
         
         // status -> $XDSSubmissionSetStatus 
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

         final QueryRegistry queryRegistry = new QueryRegistry(query);
         queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);

         return queryRegistry;
     
 }
}
