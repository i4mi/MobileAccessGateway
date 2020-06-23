package ch.bfh.ti.i4mi.mag.mhd;

import java.util.ArrayList;
import java.util.List;

import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;

public class BaseRequestConverter {

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
	    
	    // TODO is this the correct mapping for URIs?
	    public static List<String> urisFromTokens(TokenOrListParam params) {
	    	if (params == null) return null;
	    	List<String> result = new ArrayList<String>();
	    	for (TokenParam token : params.getValuesAsQueryTokens()) {
	    		result.add(token.getValue());
	    	}
	    	return result;    	
	    }
}
