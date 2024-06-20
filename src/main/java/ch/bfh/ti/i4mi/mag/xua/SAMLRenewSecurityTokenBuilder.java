/*
 * Copyright 2020 the original author or authors.
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

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.camel.Body;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpMessage;
import org.apache.commons.httpclient.HttpClient;
import org.apache.cxf.staxutils.StaxUtils;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.ws.soap.client.BasicSOAPMessageContext;
import org.opensaml.ws.soap.client.http.HttpClientBuilder;
import org.opensaml.ws.soap.client.http.HttpSOAPClient;
import org.opensaml.ws.soap.client.http.TLSProtocolSocketFactory;
import org.opensaml.ws.soap.common.SOAPException;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.ws.soap.soap11.Header;
import org.opensaml.ws.wssecurity.BinarySecurityToken;
import org.opensaml.ws.wssecurity.Created;
import org.opensaml.ws.wssecurity.EncodedString;
import org.opensaml.ws.wssecurity.Expires;
import org.opensaml.ws.wssecurity.Security;
import org.opensaml.ws.wssecurity.SecurityTokenReference;
import org.opensaml.ws.wssecurity.Timestamp;
import org.opensaml.ws.wssecurity.WSSecurityConstants;
import org.opensaml.ws.wssecurity.util.WSSecurityHelper;
import org.opensaml.ws.wstrust.RenewTarget;
import org.opensaml.ws.wstrust.RequestSecurityToken;
import org.opensaml.ws.wstrust.RequestType;
import org.opensaml.ws.wstrust.TokenType;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.security.keyinfo.KeyInfoHelper;
import org.opensaml.xml.security.x509.X509Credential;

import org.opensaml.xml.signature.DocumentInternalIDContentReference;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.trust.X509KeyManager;
import org.springframework.security.saml.trust.X509TrustManager;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.MetadataCriteria;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;

@Slf4j
@Component
public class SAMLRenewSecurityTokenBuilder {

    private static SecureRandomIdentifierGenerator secureRandomIdGenerator;
    
    static {
        try {
            secureRandomIdGenerator = new SecureRandomIdentifierGenerator();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }
    }

    private final KeyManager keyManager;
    private final SAMLContextProvider contextProvider;
    private final String keyAlias;
    private final Map<String, IDPConfig> idps;

    public SAMLRenewSecurityTokenBuilder(final KeyManager keyManager,
                                         final SAMLContextProvider contextProvider,
                                         final @Value("${mag.iua.idp.key-alias}") String keyAlias,
                                         final @Qualifier("idps") Map<String, IDPConfig> idps) {
        this.keyManager = keyManager;
        this.contextProvider = contextProvider;
        this.keyAlias = keyAlias;
        this.idps = idps;
    }
    
    public Element addSecurityHeader(String input) throws XMLStreamException {
        return StaxUtils.read(new StringReader(input)).getDocumentElement();
    }

    public static Processor keepRequest() {
        return exchange -> {
            HttpServletRequest request = exchange.getIn(HttpMessage.class).getRequest();
            HttpServletResponse response = exchange.getIn(HttpMessage.class).getResponse();
            exchange.setProperty("request", request);
            exchange.setProperty("response", response);                   
        };
    }
            
    public String requestRenewToken(final @Body ch.bfh.ti.i4mi.mag.xua.AssertionRequest request,
                                    final @ExchangeProperty("request") HttpServletRequest hrequest,
                                    final @ExchangeProperty("response") HttpServletResponse hresponse) throws Exception {
        final SAMLMessageContext context = this.contextProvider.getLocalAndPeerEntity(hrequest, hresponse);
        final var localEntityId = context.getLocalEntityId();
        if (!localEntityId.contains("/alias/")) {
            throw new AuthException(400, "invalid_request", "Local entity ID does not contain alias");
        }
        final var idpAlias = localEntityId.substring(localEntityId.lastIndexOf("/alias/") + 7);
        final var idp = this.idps.get(idpAlias);
        if (idp == null || idp.getRenewUrl() == null) {
            log.debug("No renew URL for IDP alias '{}'", idpAlias);
            throw new AuthException(400, "invalid_request", "IDP not found or no renew URL configured");
        }
        log.debug("IDP alias '{}', renew URL '{}'", idpAlias, idp.getRenewUrl());
        
        Object token = request.getSamlToken();
        if (token instanceof String) {
            token = addSecurityHeader((String) token);
        }

        final RequestSecurityToken renewSecurityTokenRequestMessage = createSAMLObject(RequestSecurityToken.class);
        final RequestType requestType = createSAMLObject(RequestType.class);
        requestType.setValue(RequestType.RENEW);
        renewSecurityTokenRequestMessage.getUnknownXMLObjects().add(requestType);
        final TokenType tokenType = createSAMLObject(TokenType.class);
        tokenType.setValue("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
        renewSecurityTokenRequestMessage.getUnknownXMLObjects().add(tokenType);  
        
        final RenewTarget renewTarget = createSAMLObject(RenewTarget.class);
        renewTarget.setUnknownXMLObject(unmarshall((Element) token));
        renewSecurityTokenRequestMessage.getUnknownXMLObjects().add(renewTarget);

        final Envelope envelope = createSAMLObject(Envelope.class);
        // The body element contains the actual SAML message.
        final org.opensaml.ws.soap.soap11.Body body = createSAMLObject(org.opensaml.ws.soap.soap11.Body.class);

        final String ref = randomId();
        WSSecurityHelper.addWSUId(body, ref);
                
        //WSSecuritySupport.addWSUId(body, securityModule.buildId());
        body.getUnknownXMLObjects().add(renewSecurityTokenRequestMessage);
        envelope.setBody(body);
        // Application-specific context information (for example, security or encryption information).
        final Header header = createSAMLObject(Header.class);
        envelope.setHeader(header);
        final Security security = createSAMLObject(Security.class);
        header.getUnknownXMLObjects().add(security);
        // Security timestamp defining the lifetime of the message.
        final Timestamp timestamp = createSAMLObject(Timestamp.class);
        timestamp.setWSUId(randomId());
        final Created created = createSAMLObject(Created.class);
        created.setDateTime(DateTime.now());
        timestamp.setCreated(created);
        final Expires expires = createSAMLObject(Expires.class);
        expires.setDateTime(created.getDateTime().plusSeconds(5*60));
        timestamp.setExpires(expires);
        security.getUnknownXMLObjects().add(timestamp);

        final X509Certificate publicCertificate = keyManager.getCertificate(keyAlias);
        log.debug("CERT NOT NULL:"+publicCertificate.toString());

        final BinarySecurityToken binarySecurityToken = createSAMLObject(BinarySecurityToken.class);
        binarySecurityToken.setEncodingType(EncodedString.ENCODING_TYPE_BASE64_BINARY);
        // Why not use BinarySecurityToken.setValueType()? It was not being valued in the XML by the marshaller.
        binarySecurityToken.getUnknownAttributes().put(new QName("ValueType"), WSSecurityConstants.X509_V3);
        binarySecurityToken.setValue(encode(publicCertificate));
        security.getUnknownXMLObjects().add(binarySecurityToken);
        // Digital signature to verify the identity of the signer.


        final Signature signature = createSAMLObject(Signature.class);
        final Credential cred = keyManager.getCredential(keyAlias);
        signature.setSigningCredential(cred);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        final DocumentInternalIDContentReference timestampReference =
                new DocumentInternalIDContentReference(timestamp.getWSUId());
        timestampReference.getTransforms().add(CanonicalizationMethod.EXCLUSIVE);
        timestampReference.setDigestAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        signature.getContentReferences().add(timestampReference);

        final DocumentInternalIDContentReference bodyReference =
                new DocumentInternalIDContentReference(WSSecurityHelper.getWSUId(body));
        bodyReference.getTransforms().add(CanonicalizationMethod.EXCLUSIVE);
        bodyReference.setDigestAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        signature.getContentReferences().add(bodyReference);
        final KeyInfo keyInfo = createSAMLObject(KeyInfo.class);
        final SecurityTokenReference securityTokenReference = createSAMLObject(SecurityTokenReference.class);
        keyInfo.getXMLObjects().add(securityTokenReference);


        final X509Data x509Data = createSAMLObject(X509Data.class);
       
        x509Data.getX509IssuerSerials().add(KeyInfoHelper.buildX509IssuerSerial(
                publicCertificate.getIssuerX500Principal().getName(), publicCertificate.getSerialNumber()));
        securityTokenReference.getUnknownXMLObjects().add(x509Data);
       
        signature.setKeyInfo(keyInfo);
        security.getUnknownXMLObjects().add(signature);
        
        marshall(envelope);
        sign(signature);      
      
        // Build the W3C DOM representing the SOAP message.
        final Element elem = marshall(envelope);
        
        log.debug(StaxUtils.toString(elem));

        final Envelope result = send(idp.getRenewUrl(), context, idp, envelope);
        final NodeList lst = result.getBody().getDOM().getElementsByTagNameNS(SAML20_NS,"Assertion");
                         
        final Node node = lst.item(0);
        log.debug("NODE: "+node.toString());
        return StaxUtils.toString(node);
    }
    
    public static String encode(final X509Certificate certificate) throws CertificateEncodingException {
        final Base64.Encoder encoder = Base64.getEncoder();

        final byte[] rawCrtText = certificate.getEncoded();
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        
        return encodedCertText;
    }
    
    public static void sign(final Signature signature) {
        try {
            Signer.signObject(signature);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }
   
    public static <T> T createSAMLObject(final Class<T> clazz) {
        T object = null;
        try {
            XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
            QName defaultElementName;
            
            try {
               defaultElementName = (QName)clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
            } catch (NoSuchFieldException e) {
               defaultElementName = (QName)clazz.getDeclaredField("ELEMENT_NAME").get(null);
            }
            
            object = (T)builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        } catch (IllegalAccessException e) {            
            throw new IllegalArgumentException("Could not create SAML object");
        } catch (NoSuchFieldException e) {
            log.error("Exception", e);
            log.error("CLASSNAME: "+clazz.getName());
            throw new IllegalArgumentException("Could not create SAML object");
        }

        return object;
    }
    
    public Element marshall(final XMLObject object) {
        if (object instanceof SignableSAMLObject && ((SignableSAMLObject) object).isSigned()
                && object.getDOM() != null) {
           return object.getDOM();
        } else {
           try {
             Marshaller out = Configuration.getMarshallerFactory().getMarshaller(object);
             out.marshall(object);
             return object.getDOM();
           } catch (MarshallingException e) {
             return null;       
           }
       }
    }
    
    public XMLObject unmarshall(final Element input) throws UnmarshallingException {
        Unmarshaller un = Configuration.getUnmarshallerFactory().getUnmarshaller(input);
        return un.unmarshall(input);
    }

    public static String randomId() {
        return secureRandomIdGenerator.generateIdentifier();
    }

    private Envelope send(final String targetUrl,
                          final SAMLMessageContext context,
                          final IDPConfig idp,
                          final Envelope envelope) throws SOAPException, org.opensaml.xml.security.SecurityException {
        final var clientBuilder = new HttpClientBuilder();

        final var criteriaSet = new CriteriaSet();
        criteriaSet.add(new EntityIDCriteria(context.getPeerEntityId()));
        criteriaSet.add(new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS));
        criteriaSet.add(new UsageCriteria(UsageType.UNSPECIFIED));

        final var trustManager = new X509TrustManager(criteriaSet, context.getLocalSSLTrustEngine());
        final var manager = new X509KeyManager((X509Credential) this.keyManager.getCredential(idp.getTlsKeyAlias()));
        clientBuilder.setHttpsProtocolSocketFactory(new TLSProtocolSocketFactory(manager, trustManager));

        final HttpClient httpClient = clientBuilder.buildClient();
        final var soapClient = new HttpSOAPClient(httpClient, new BasicParserPool());
        final var soapContext = new BasicSOAPMessageContext();
        soapContext.setOutboundMessage(envelope);
        soapClient.send(targetUrl, soapContext);

        return (Envelope)soapContext.getInboundMessage();
    }
}
