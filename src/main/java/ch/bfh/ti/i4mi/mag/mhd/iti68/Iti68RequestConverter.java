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

package ch.bfh.ti.i4mi.mag.mhd.iti68;

import java.util.Map;

import org.apache.camel.Headers;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RetrieveDocumentSet;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

/**
 * ITI-68 to ITI-43 request converter
 * @author alexander kreutz
 *
 */
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
