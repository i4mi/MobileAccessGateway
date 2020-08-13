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

import org.apache.camel.Header;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

/**
 * ITI-67 to ITI-18 request converter
 * 
 * @author oliver egger
 *
 */
public class IdRequestConverter extends BaseRequestConverter {

    /**
     * convert ITI-67 request to ITI-18 request
     * 
     * @param searchParameter
     * @return
     */
    public QueryRegistry idToGetDocumentsQuery(@Header(value = "FhirHttpUri") String fhirHttpUri) {

        if (fhirHttpUri != null && fhirHttpUri.contains("/")) {
            boolean getLeafClass = true;
            String uniqueId = fhirHttpUri.substring(fhirHttpUri.lastIndexOf("/") + 1);

            GetDocumentsQuery query = new GetDocumentsQuery();
            final QueryRegistry queryRegistry = new QueryRegistry(query);
            List<String> uniqueIds = new ArrayList<String>();
            uniqueIds.add(uniqueId);
            query.setUniqueIds(uniqueIds);
            queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);
            return queryRegistry;
        }
        return null;

    }
}
