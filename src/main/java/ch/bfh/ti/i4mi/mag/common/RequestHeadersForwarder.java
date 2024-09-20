package ch.bfh.ti.i4mi.mag.common;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.CastUtils;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.util.*;

import static ch.bfh.ti.i4mi.mag.xua.XuaUtils.OASIS_WSSECURITY_NS;
import static org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint.OUTGOING_SOAP_HEADERS;
import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;

/**
 * This processor forwards the HTTP request headers (from FHIR) to the SOAP request headers. This is needed for
 * Authorization and traceparent headers.
 *
 * @author Quentin Ligier
 **/
public class RequestHeadersForwarder {
    private static final Logger log = LoggerFactory.getLogger(RequestHeadersForwarder.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public static Processor forward() {
        return exchange -> {
            // Forward the Authorization header if present
            forwardAuthToken(exchange);

            // Extract the traceparent header if present and update it for the next hop
            TraceparentHandler.saveHeader(exchange);
            TraceparentHandler.updateHeaderForSoap().process(exchange);
        };
    }

    public static Processor checkAuthorization(final boolean check) {
        return exchange -> {
            final var authorizationHeader = FhirExchanges.readRequestHttpHeader(AUTHORIZATION_HEADER, exchange, false);
            if (check && authorizationHeader == null) {
                throw new AuthenticationException("The Authorization header is missing");
            }
            // TODO verify if the token is valid
        };
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
    private static void forwardAuthToken(final Exchange exchange) throws XMLStreamException {
        final var authorizationHeader = FhirExchanges.readRequestHttpHeader(AUTHORIZATION_HEADER, exchange, true);
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
            newHeader.setDirection(Header.Direction.DIRECTION_OUT);

            soapHeaders.add(newHeader);

            exchange.getMessage().setHeader(OUTGOING_SOAP_HEADERS, soapHeaders);
            exchange.setProperty("UserName", userName);
        } else {
            // It is a JWT or something else, just forward it
            log.debug("Forwarding Authorization header: {}", authorizationHeader);
            SoapExchanges.writeResponseHttpHeader(AUTHORIZATION_HEADER, authorizationHeader, exchange);
        }
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
}
