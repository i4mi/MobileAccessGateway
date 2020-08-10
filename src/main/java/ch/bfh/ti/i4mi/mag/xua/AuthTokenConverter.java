package ch.bfh.ti.i4mi.mag.xua;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

//import org.apache.camel.Header;
import org.apache.camel.Processor;
import org.apache.camel.util.CastUtils;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.Header.Direction;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.utils.DOMUtil;

import static org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint.*;

public class AuthTokenConverter {

	public static String convert( String token) {
		if (token != null && token.startsWith("IHE-SAML ")) {
			String base64Token = token.substring("IHE-SAML ".length());
			byte[] decoded = Base64.getDecoder().decode(base64Token);
			return new String(decoded);			
		}
		return null;
	}
	
	public static Processor addWsHeader() {
        return exchange -> {
        	
        	Map<String, List<String>> httpHeaders = (Map<String, List<String>>) exchange.getIn().getHeader("FhirHttpHeaders");
        	for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
        		System.out.println("XXX: "+entry.getKey()+" = "+(entry.getValue()!=null ? entry.getValue().toString(): "null"));
        	}
        	
        	if (httpHeaders != null) {
        	List<String> header = httpHeaders.get("Authorization");
        	System.out.println("CHECK HEADER");
        	if (header != null) {
        		System.out.println("CHECK HEADER1");
        		String converted = convert(header.get(0)); 
        	System.out.println("FOUND:"+converted);
        	
        	List<SoapHeader> soapHeaders = CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));
            SoapHeader newHeader;
     
            if(soapHeaders == null){     
                soapHeaders = new ArrayList<SoapHeader>();
            }
             
            //try {
                newHeader = new SoapHeader(new QName("soapHeader"), StaxUtils.read(new StringReader(converted)).getDocumentElement());
                newHeader.setDirection(Direction.DIRECTION_OUT);
     
                soapHeaders.add(newHeader);
     
                exchange.getMessage().setHeader(OUTGOING_SOAP_HEADERS, soapHeaders);
     
            //} catch (Exception e) {
                //log error
            //}
        	}
        	
        	
        	}
        	/*javax.xml.namespace.QName headerName = new javax.xml.namespace.QName("http://schemas.xmlsoap.org/ws/2002/07/secext", "Security");
			new Header(headerName, "simple contents", new JAXBDataBinding(String.class));
			exchange.getMessage().getHeader(OUTGOING_SOAP_HEADERS);*/        	     	        
        };
    }
}
