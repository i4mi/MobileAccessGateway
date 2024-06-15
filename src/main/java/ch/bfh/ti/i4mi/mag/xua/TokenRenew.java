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
