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

import static ch.bfh.ti.i4mi.mag.xua.XuaUtils.OASIS_WSSECURITY_NS;
import static org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint.OUTGOING_SOAP_HEADERS;
import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.Processor;
import org.apache.camel.util.CastUtils;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.Header.Direction;
import org.apache.cxf.staxutils.StaxUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Use IHE-SAML Header from request as SOAP wsse Security Header
 * @author alexander kreutz
 *
 */
public class AuthTokenConverter {

	private static final String AUTHORIZATION_HEADER = "Authorization";

	/**
	 * This class is not instantiable.
	 */
	private AuthTokenConverter() {
	}

	public static String convert(final String token) {
		final String base64Token;
		if (token != null && token.startsWith("IHE-SAML ")) {
			base64Token = token.substring("IHE-SAML ".length());
		} else if (token != null && token.startsWith("Bearer ")) {
			base64Token = token.substring("Bearer ".length());
		} else {
			return null;
		}
		return new String(Base64.getDecoder().decode(base64Token));
	}

	private static String getNodeValue(final Element in,
									   final String ns,
									   final String element) {
		 final NodeList lst = in.getElementsByTagNameNS(ns, element);
		 if (lst.getLength() == 0) {
			 return "";
		 }
		 return lst.item(0).getTextContent();
         
	}
	
	private static String getAttrValue(final Element in,
									   final String ns,
									   final String element,
									   final String attribute) {
		 final NodeList lst = in.getElementsByTagNameNS(ns, element);
		 if (lst.getLength() == 0) {
			 return "";
		 }
		 final Node attr = lst.item(0).getAttributes().getNamedItem(attribute);
		 return attr != null ? attr.getTextContent() : "";
	}
	
	public static Processor addWsHeader() {
		return exchange -> {

			Map<String, List<String>> httpHeaders =
					CastUtils.cast((Map<?, ?>) exchange.getMessage().getHeader("FhirHttpHeaders"));

			String converted = null;

			if (httpHeaders != null) {
				final List<String> header = httpHeaders.get(AUTHORIZATION_HEADER);
				if (header != null) {
					converted = convert(header.get(0));
					httpHeaders.remove(AUTHORIZATION_HEADER);
				}
			} else {
				final Object authHeader = exchange.getMessage().getHeader(AUTHORIZATION_HEADER);
				if (authHeader != null) {
					exchange.getMessage().removeHeader(AUTHORIZATION_HEADER);
					converted = convert(authHeader.toString());
				}
			}
			if (converted != null) {				               
			    if (converted.startsWith("<?xml")) {
					converted = converted.substring(converted.indexOf(">") + 1);
				}
				converted = String.format("<wsse:Security xmlns:wsse=\"%s\">%s</wsse:Security>", OASIS_WSSECURITY_NS, converted);
				
				List<SoapHeader> soapHeaders =
						CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));

				if (soapHeaders == null) {
					soapHeaders = new ArrayList<>(1);
				}
                final Element headerDocument = StaxUtils.read(new StringReader(converted)).getDocumentElement();
                
                String alias = getAttrValue(headerDocument, SAML20_NS, "NameID", "SPProvidedID");
                String user = getNodeValue(headerDocument, SAML20_NS, "NameID");
                String issuer = getNodeValue(headerDocument, SAML20_NS, "Issuer");
                
                String userName = alias+"<"+user+"@"+issuer+">";                
                               
				final var newHeader = new SoapHeader(new QName(OASIS_WSSECURITY_NS, "Security"), headerDocument);
				newHeader.setDirection(Direction.DIRECTION_OUT);

				soapHeaders.add(newHeader);
				
				exchange.getMessage().setHeader(OUTGOING_SOAP_HEADERS, soapHeaders);
				exchange.setProperty("UserName", userName);
			}
		};
	}
}
