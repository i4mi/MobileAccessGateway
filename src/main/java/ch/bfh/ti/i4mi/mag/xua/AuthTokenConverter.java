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

import org.apache.camel.Processor;
import org.apache.camel.util.CastUtils;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.Header.Direction;
import org.apache.cxf.staxutils.StaxUtils;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.StringReader;
import java.util.*;

import static ch.bfh.ti.i4mi.mag.xua.XuaUtils.OASIS_WSSECURITY_NS;
import static org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint.OUTGOING_SOAP_HEADERS;
import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;


/**
 * Use IHE-SAML Header from request as SOAP wsse Security Header
 *
 * @author alexander kreutz
 */
public class AuthTokenConverter {
    private static final Logger log = LoggerFactory.getLogger(AuthTokenConverter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * This class is not instantiable.
     */
    private AuthTokenConverter() {
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

    /**
     * Forwards the Authorization header to the next hop.
     * <p>
     * If the Authorization header contains a base64-encoded SAML assertion, it is decoded and
     * a WS-Security header is created from it.
     * </p>
     * <p>
     * If the Authorization header is a JWT or something else, it is forwarded as is.
     * </p>
     */
    public static Processor forwardAuthToken() {
        return exchange -> {
            Map<String, List<String>> httpHeaders =
                    CastUtils.cast((Map<?, ?>) exchange.getMessage().getHeader(Constants.HTTP_INCOMING_HEADERS));

            // Find the Authorization header in the HTTP headers
            String authorizationHeader = null;
            if (httpHeaders != null) {
                final List<String> header = httpHeaders.get(AUTHORIZATION_HEADER);
                if (header != null) {
                    authorizationHeader = header.get(0);
                    httpHeaders.remove(AUTHORIZATION_HEADER);
                }
            } else {
                final Object authHeader = exchange.getMessage().getHeader(AUTHORIZATION_HEADER);
                if (authHeader != null) {
                    authorizationHeader = authHeader.toString();
                    exchange.getMessage().removeHeader(AUTHORIZATION_HEADER);
                }
            }

            if (authorizationHeader == null) {
                return;
            }

            // Extract the payload from the Authorization header
            final String payload;
            if (authorizationHeader.startsWith("Bearer ")) {
                payload = authorizationHeader.substring("Bearer ".length());
            } else if (authorizationHeader.startsWith("IHE-SAML ")) {
                payload = authorizationHeader.substring("IHE-SAML ".length());
            } else {
                return;
            }

            if (payload.startsWith("PHNhbWwyOkFzc2") || payload.startsWith("PD94bW")) {
                // It is an encoded SAML assertion, convert it to a WS-Security header
                log.debug("Converting encoded SAML assertion to WS-Security header");
                String converted = new String(Base64.getDecoder().decode(payload));
                if (converted.startsWith("<?xml")) {
                    converted = converted.substring(converted.indexOf(">") + 1);
                }
                converted = String.format("<wsse:Security xmlns:wsse=\"%s\">%s</wsse:Security>",
                                          OASIS_WSSECURITY_NS,
                                          converted);

                List<SoapHeader> soapHeaders =
                        CastUtils.cast((List<?>) exchange.getIn().getHeader(Header.HEADER_LIST));

                if (soapHeaders == null) {
                    soapHeaders = new ArrayList<>(1);
                }
                final Element headerDocument = StaxUtils.read(new StringReader(converted)).getDocumentElement();

                String alias = getAttrValue(headerDocument, SAML20_NS, "NameID", "SPProvidedID");
                String user = getNodeValue(headerDocument, SAML20_NS, "NameID");
                String issuer = getNodeValue(headerDocument, SAML20_NS, "Issuer");

                String userName = alias + "<" + user + "@" + issuer + ">";

                final var newHeader = new SoapHeader(new QName(OASIS_WSSECURITY_NS, "Security"), headerDocument);
                newHeader.setDirection(Direction.DIRECTION_OUT);

                soapHeaders.add(newHeader);

                exchange.getMessage().setHeader(OUTGOING_SOAP_HEADERS, soapHeaders);
                exchange.setProperty("UserName", userName);
            } else {
                // It is a JWT or something else, just forward it
                log.debug("Forwarding Authorization header: {}", authorizationHeader);
                final Map<String, String> outgoingHttpHeaders =
                        CastUtils.cast(exchange.getMessage().getHeader(AbstractWsEndpoint.OUTGOING_HTTP_HEADERS,
                                                                       HashMap::new, Map.class));

                outgoingHttpHeaders.put(AUTHORIZATION_HEADER, authorizationHeader);


            }
        };
    }
}
