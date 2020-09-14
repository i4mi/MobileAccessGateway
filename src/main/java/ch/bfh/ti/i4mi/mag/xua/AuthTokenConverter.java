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
		return null;
	}

	public static Processor addWsHeader() {
		return exchange -> {

			Map<String, List<String>> httpHeaders = (Map<String, List<String>>) exchange.getIn().getHeader("FhirHttpHeaders");

			String converted = null;

			if (httpHeaders != null) {
				List<String> header = httpHeaders.get("Authorization");
				if (header != null) {
					converted = convert(header.get(0));
				}
			} else {
				Object authHeader = exchange.getIn().getHeader("Authorization");
				if (authHeader != null)
					converted = convert(authHeader.toString());
			}
			if (converted != null) {
				converted = "<wsse:Security xmlns:wsse=\\\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\\\">"+converted+"</wsse:security>";
				List<SoapHeader> soapHeaders = CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));
				SoapHeader newHeader;

				if (soapHeaders == null) {
					soapHeaders = new ArrayList<SoapHeader>();
				}

				newHeader = new SoapHeader(new QName("soapHeader"), StaxUtils.read(new StringReader(converted)).getDocumentElement());
				newHeader.setDirection(Direction.DIRECTION_OUT);

				soapHeaders.add(newHeader);

				exchange.getMessage().setHeader(OUTGOING_SOAP_HEADERS, soapHeaders);

			}

		};
	}
}
