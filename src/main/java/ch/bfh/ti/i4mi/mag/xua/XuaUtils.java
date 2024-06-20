package ch.bfh.ti.i4mi.mag.xua;

import org.apache.camel.Body;
import org.apache.cxf.staxutils.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;

/**
 * MobileAccessGateway
 *
 * @author Quentin Ligier
 **/
public class XuaUtils {
    private static final Logger log = LoggerFactory.getLogger(XuaUtils.class);

    public static final String OASIS_WSSECURITY_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    public String extractAssertionAsString(final @Body SOAPMessage in) throws SOAPException {
        log.debug("INPUT: {}", in);
        SOAPBody body = in.getSOAPBody();
        NodeList lst = body.getElementsByTagNameNS(SAML20_NS, "Assertion");
        Node node = lst.item(0);
        log.debug("NODE: {}", node);
        // TODO: omit xml declaration?
        return StaxUtils.toString(node);
    }
}
