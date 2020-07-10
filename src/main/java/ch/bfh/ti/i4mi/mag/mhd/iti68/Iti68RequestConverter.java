package ch.bfh.ti.i4mi.mag.mhd.iti68;

import java.util.Map;

import org.apache.camel.Headers;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

public class Iti68RequestConverter extends BaseRequestConverter {


    public static RetrieveDocumentSet queryParameterToRetrieveDocumentSet(@Headers Map<String, Object> parameters) {
            	           
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
            
            return retrieveDocumentSet;        
    }
    
}
