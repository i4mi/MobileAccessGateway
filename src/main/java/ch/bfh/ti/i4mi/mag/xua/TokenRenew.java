package ch.bfh.ti.i4mi.mag.xua;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;

import org.apache.camel.Body;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Header;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.xml.security.utils.XMLUtils;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;



import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TokenRenew {

    private String BASE_MSG() { return """
    <?xml version="1.0" ?>
    <soap12:Envelope xmlns:soap12="http://www.w3.org/2003/05/soap-envelope" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
       <soap12:Body>
         <wst:RequestSecurityToken xmlns:wst="http://docs.oasis-open.org/ws-sx/ws-trust/200512">
           <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Renew</wst:RequestType>
           <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>
           <wst:RenewTarget></wst:RenewTarget>
         </wst:RequestSecurityToken>
       </soap12:Body>
    </soap12:Envelope>
     """; }
    
    public Element addSecurityHeader(String input) throws XMLStreamException {
        return (Element) StaxUtils.read(new StringReader(input)).getDocumentElement();
    }
    
    public SOAPMessage buildRenewRequest(@Body AssertionRequest request) throws SOAPException, IOException, XMLStreamException, AuthException {
        
        Object token = request.getSamlToken();
        if (token == null) throw new AuthException(400, "server_error", "No SAML token found");
        log.info(token.getClass().getSimpleName());
        if (token instanceof String && token.toString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) token = token.toString().substring("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".length());
        log.info("Decoded IDP Token:"+token);
               
        MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage message = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(BASE_MSG().getBytes(Charset.forName("UTF-8"))));
        log.info("BASE MSG");
        // message.getSOAPHeader().addChildElement("MessageID","wsa","http://www.w3.org/2005/08/addressing").addTextNode(UUID.randomUUID().toString());
        
        SOAPElement renewTarget = (SOAPElement) message.getSOAPBody().getElementsByTagNameNS("http://docs.oasis-open.org/ws-sx/ws-trust/200512", "RenewTarget").item(0);
        log.info("RENEW TG");
        Element elem;
        if (token instanceof String) {
            elem = addSecurityHeader(token.toString());        
        } else {           
            elem = ((Element) token);
        }
        Node node = message.getSOAPBody().getOwnerDocument().importNode(elem, true);
        log.info("RENEW NODE");
        renewTarget.appendChild(node);
                                    
        log.info("Sending IDP Renew Request: "+message.toString());
        
        message.saveChanges();
                        
        return message;
    }
    
    public AssertionRequest buildAssertionRequest(@Header("assertionRequest") AssertionRequest assertionRequest, @Body String renewedIdpAssertion) {
        assertionRequest.setSamlToken(renewedIdpAssertion);
        return assertionRequest;
    }
    
    public AuthenticationRequest emptyAuthRequest() {
        return new AuthenticationRequest();
    }
    
    public AssertionRequest keepIdpAssertion(@ExchangeProperty("oauthrequest") AuthenticationRequest authRequest, @Body AssertionRequest assertionRequest) {
        String idpAssertion;
        if (assertionRequest.getSamlToken() instanceof String) {
            idpAssertion = (String) assertionRequest.getSamlToken(); 
        } else {
            idpAssertion = XMLHelper.nodeToString((Node) assertionRequest.getSamlToken());
        }
        authRequest.setIdpAssertion(idpAssertion);
        return assertionRequest;
    }
}
