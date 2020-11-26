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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.apache.camel.Processor;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.RetrieveDocumentSetResponseType.DocumentResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.ErrorInfo;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocument;
import org.openehealth.ipf.commons.ihe.xds.core.responses.RetrievedDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;

import ca.uhn.fhir.rest.api.Constants;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;

/**
 * ITI-68 from ITI-43 response converter
 * @author alexander kreutz
 *
 */
public class Iti68ResponseConverter extends BaseResponseConverter {

	public static Object retrievedDocumentSetToHttResponse(@Body RetrievedDocumentSet retrievedDocumentSet, @Headers Map<String, Object> headers) throws IOException {
                		
            if (Status.SUCCESS.equals(retrievedDocumentSet.getStatus())) {
                List<RetrievedDocument> documentResponses = retrievedDocumentSet.getDocuments();
                if (documentResponses.size()==1) {
                	RetrievedDocument documentResponse = documentResponses.get(0);
                    final InputStream in = documentResponse.getDataHandler().getInputStream();
                    byte[] byteArray=org.apache.commons.io.IOUtils.toByteArray(in);
                    headers.clear();
                    headers.put("Content-Type", documentResponse.getMimeType());
                    return byteArray;
                }
            } else {
            	headers.put(Exchange.HTTP_RESPONSE_CODE, 400);
            	List<ErrorInfo> errors = retrievedDocumentSet.getErrors();
            	StringBuffer result = new StringBuffer();
            	for (ErrorInfo error : errors) {
            		result.append(error.getCodeContext());
            	}
            	return result;
            }
        
            return null;
    }
}
