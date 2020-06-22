package ch.bfh.ti.i4mi.mag.mhd;

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

public class BinaryFromDocumentSetResponse extends MhdFromResponse {

	public Object retrievedDocumentSetToHttResponse(@Body RetrievedDocumentSet retrievedDocumentSet, @Headers Map<String, Object> headers) throws IOException {
                		
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
