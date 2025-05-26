package ch.bfh.ti.i4mi.mag.xua;

import org.apache.camel.Body;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.util.ArrayList;
import java.util.List;

import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;

/**
 * MobileAccessGateway
 *
 * @author Quentin Ligier
 **/
public class XuaUtils {
    private static final Logger log = LoggerFactory.getLogger(XuaUtils.class);

    public static final String OASIS_WSSECURITY_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String OASIS_ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String EMAIL_ATTRIBUTE_NAME = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

    @SuppressWarnings("unused")
    public String extractAssertionAsString(final @Body SOAPMessage in) throws SOAPException {
        log.debug("INPUT: {}", in);
        SOAPBody body = in.getSOAPBody();
        NodeList lst = body.getElementsByTagNameNS(SAML20_NS, "Assertion");
        Node node = lst.item(0);
        log.debug("NODE: {}", node);
        // TODO: omit xml declaration?
        return StaxUtils.toString(node);
    }

    @Nullable
    public static String getEmailFromIdpToken(final Element assertion) {
        if (assertion == null) {
            return null;
        }

        final var emailAttribute = getElementsByTagNameNS(assertion,
                                                          OASIS_ASSERTION_NS,
                                                          "Attribute")
                .stream()
                .filter(attr -> EMAIL_ATTRIBUTE_NAME.equals(attr.getAttribute("Name")))
                .findFirst()
                .orElse(null);
        if (emailAttribute == null) {
            log.debug("No email attribute found in the assertion");
            return null;
        }
        final var attributeValueNodes = emailAttribute.getElementsByTagNameNS(OASIS_ASSERTION_NS,
                                                                              "AttributeValue");
        if (attributeValueNodes.getLength() != 1) {
            log.debug("Expected exactly one AttributeValue for email, found: {}", attributeValueNodes.getLength());
            return null;
        }
        final var attributeValueElement = (Element) attributeValueNodes.item(0);
        return attributeValueElement.getTextContent().trim();
    }

    private static List<Element> getElementsByTagNameNS(final Element element,
                                                        final String namespaceURI,
                                                        final String localName) {
        final NodeList nodeList = element.getElementsByTagNameNS(namespaceURI, localName);
        final var elements = new ArrayList<Element>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }
        return elements;
    }
}
