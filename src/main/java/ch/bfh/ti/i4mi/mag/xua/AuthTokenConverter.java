/*
 * Copyright 2015 the original author or authors.
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

package ch.bfh.ti.i4mi.mag.xua;

import static org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint.OUTGOING_SOAP_HEADERS;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.Message;
//import org.apache.camel.Header;
import org.apache.camel.Processor;
import org.apache.camel.util.CastUtils;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.Header.Direction;
import org.apache.cxf.staxutils.StaxUtils;
import org.openehealth.ipf.commons.ihe.ws.cxf.audit.AbstractAuditInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.audit.AuditInRequestInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.audit.AuditOutRequestInterceptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Use IHE-SAML Header from request as SOAP wsse Security Header
 * @author alexander kreutz
 *
 */
public class AuthTokenConverter {

	public static String convert(String token) {
		if (token != null && token.startsWith("IHE-SAML ")) {
			String base64Token = token.substring("IHE-SAML ".length());
			byte[] decoded = Base64.getDecoder().decode(base64Token);
			return new String(decoded);
		}
		if (token != null && token.startsWith("Bearer ")) {
			String base64Token = token.substring("Bearer ".length());
			byte[] decoded = Base64.getDecoder().decode(base64Token);
			return new String(decoded);
		}
		return null;
	}

	private static String getNodeValue(Element in, String ns, String element) {
		 NodeList lst = in.getElementsByTagNameNS(ns, element);
		 if (lst.getLength()==0) return "";
		 return lst.item(0).getTextContent();
         
	}
	
	private static String getAttrValue(Element in, String ns, String element, String attribute) {
		 NodeList lst = in.getElementsByTagNameNS(ns, element);
		 if (lst.getLength()==0) return "";
		 Node attr = lst.item(0).getAttributes().getNamedItem(attribute);
		 return attr != null ? attr.getTextContent() : ""; 
        
	}
	
	public static Processor addWsHeader() {
		return exchange -> {

			Map<String, List<String>> httpHeaders = (Map<String, List<String>>) exchange.getMessage().getHeader("FhirHttpHeaders");

			String converted = null;

			if (httpHeaders != null) {
				List<String> header = httpHeaders.get("Authorization");
				if (header != null) {
					converted = convert(header.get(0));
					httpHeaders.remove("Authorization");
				}
			} else {
				Object authHeader = exchange.getMessage().getHeader("Authorization");
				if (authHeader != null) {
					exchange.getMessage().removeHeader("Authorization");
					converted = convert(authHeader.toString());
				}
			}
			if (converted != null) {				               
			    if (converted.startsWith("<?xml")) converted = converted.substring(converted.indexOf(">")+1);		
				converted = "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"+converted+"</wsse:Security>";

				//System.out.println(converted);
				List<SoapHeader> soapHeaders = CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));
				SoapHeader newHeader;

				if (soapHeaders == null) {
					soapHeaders = new ArrayList<SoapHeader>();
				}
                Element headerDocument = StaxUtils.read(new StringReader(converted)).getDocumentElement();
                
                String alias = getAttrValue(headerDocument, "urn:oasis:names:tc:SAML:2.0:assertion", "NameID", "SPProvidedID");
                String user = getNodeValue(headerDocument, "urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
                String issuer = getNodeValue(headerDocument, "urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
                
                String userName = alias+"<"+user+"@"+issuer+">";                
                               
				newHeader = new SoapHeader(new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security"), headerDocument);
				newHeader.setDirection(Direction.DIRECTION_OUT);

				soapHeaders.add(newHeader);
				Message msg = exchange.getMessage();
				
				exchange.getMessage().setHeader(OUTGOING_SOAP_HEADERS, soapHeaders);
				exchange.setProperty("UserName", userName);
				
			}

		};
	}
}
