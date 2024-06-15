package ch.bfh.ti.i4mi.mag.xua;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpMessage;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.soap.client.BasicSOAPMessageContext;
import org.opensaml.ws.soap.client.http.HttpClientBuilder;
import org.opensaml.ws.soap.client.http.HttpSOAPClient;
import org.opensaml.ws.soap.client.http.TLSProtocolSocketFactory;
import org.opensaml.ws.soap.common.SOAPException;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.ws.soap.soap11.Header;
import org.opensaml.ws.soap.util.SOAPHelper;
import org.opensaml.ws.transport.http.HttpClientInTransport;
import org.opensaml.ws.transport.http.HttpClientOutTransport;
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
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509Credential;

import org.opensaml.xml.signature.DocumentInternalIDContentReference;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.SignableXMLObject;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Data;
import org.opensaml.xml.signature.X509IssuerSerial;
import org.opensaml.xml.signature.impl.X509IssuerSerialBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.processor.SAMLProcessor;
import org.springframework.security.saml.trust.X509KeyManager;
import org.springframework.security.saml.trust.X509TrustManager;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.binding.encoding.HTTPSOAP11Encoder;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.security.MetadataCriteria;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
public class ScSAMLRenewSecurityTokenBuilder {
    
    private String renewEndpointUrl =  "https://samlservices.test.epr.fed.hin.ch/saml/2.0/renewassertion";                                                                              
    //private String renewEndpointUrl =  "https://test.ahdis.ch/eprik-cara/camel/hin/ahdis/saml/2.0/renewassertion";
    
    private static SecureRandomIdentifierGenerator secureRandomIdGenerator;
    
    private String destination;
    
    static {
        try {
            secureRandomIdGenerator = new SecureRandomIdentifierGenerator();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }
    }
 
    
    @Autowired
    private KeyManager keyManager;     
    
    @Autowired
    private SAMLContextProvider contextProvider;
    
    @Autowired
    SAMLProcessor processor;
    
    //@Autowired
    //private HttpClient httpClient;
    
    @Value("${mag.iua.idp.key-alias}")
    private String keyAlias;
    
    public Element addSecurityHeader(String input) throws XMLStreamException {
        return (Element) StaxUtils.read(new StringReader(input)).getDocumentElement();
    }

    public static Processor keepRequest() {
        return exchange -> {
            HttpServletRequest request = exchange.getIn(HttpMessage.class).getRequest();
            HttpServletResponse response = exchange.getIn(HttpMessage.class).getResponse();
            exchange.setProperty("request", request);
            exchange.setProperty("response", response);                   
        };
    }
            
    public String requestRenewToken(@Body ch.bfh.ti.i4mi.mag.xua.AssertionRequest request, @ExchangeProperty("request") HttpServletRequest hrequest, @ExchangeProperty("response") HttpServletResponse hresponse) throws Exception {
        
        SAMLMessageContext context = contextProvider.getLocalAndPeerEntity(hrequest, hresponse);
        
        Object token = request.getSamlToken();
        
        if (token instanceof String) {
            token = addSecurityHeader((String) token);
        }
                
        RequestSecurityToken renewSecurityTokenRequestMessage;
        
        renewSecurityTokenRequestMessage = createSAMLObject(RequestSecurityToken.class);
        RequestType requestType = createSAMLObject(RequestType.class);
        requestType.setValue(RequestType.RENEW);
        renewSecurityTokenRequestMessage.getUnknownXMLObjects().add(requestType);
        TokenType tokenType = createSAMLObject(TokenType.class);
        tokenType.setValue("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
        renewSecurityTokenRequestMessage.getUnknownXMLObjects().add(tokenType);  
        
        RenewTarget renewTarget = createSAMLObject(RenewTarget.class);
        renewTarget.setUnknownXMLObject(unmarshall((Element) token));
        renewSecurityTokenRequestMessage.getUnknownXMLObjects().add(renewTarget);
        
        
        Envelope envelope = createSAMLObject(Envelope.class);
        // The body element contains the actual SAML message.
        org.opensaml.ws.soap.soap11.Body body = createSAMLObject(org.opensaml.ws.soap.soap11.Body.class);
        
        String ref = randomId();
        WSSecurityHelper.addWSUId(body, ref);
                
        //WSSecuritySupport.addWSUId(body, securityModule.buildId());
        body.getUnknownXMLObjects().add(renewSecurityTokenRequestMessage);
        envelope.setBody(body);
        // Application-specific context information (for example, security or encryption information).
        Header header = createSAMLObject(Header.class);
        envelope.setHeader(header);
        Security security = createSAMLObject(Security.class);
        header.getUnknownXMLObjects().add(security);
        // Security timestamp defining the lifetime of the message.
        Timestamp timestamp = createSAMLObject(Timestamp.class);
        timestamp.setWSUId(randomId());
        Created created = createSAMLObject(Created.class);
        created.setDateTime(DateTime.now());
        timestamp.setCreated(created);
        Expires expires = createSAMLObject(Expires.class);
        expires.setDateTime(created.getDateTime().plusSeconds(5*60));
        timestamp.setExpires(expires);
        security.getUnknownXMLObjects().add(timestamp);
        
        // Binary security token is the base64 encoded representation of an X.509 public certificate.
        //KeyStore.PrivateKeyEntry privateKeyEntry = securityModule.findPrivateKey();
        //X509Certificate publicCertificate = (X509Certificate) privateKeyEntry.getCertificate();
        
        X509Certificate publicCertificate = keyManager.getCertificate(keyAlias);
        log.info("CERT NOT NULL:"+publicCertificate.toString());
        
        BinarySecurityToken binarySecurityToken = createSAMLObject(BinarySecurityToken.class);
        binarySecurityToken.setEncodingType(EncodedString.ENCODING_TYPE_BASE64_BINARY);
        // Why not use BinarySecurityToken.setValueType()? It was not being valued in the XML by the marshaller.
        binarySecurityToken.getUnknownAttributes().put(new QName("ValueType"), WSSecurityConstants.X509_V3);
        binarySecurityToken.setValue(encode(publicCertificate));
        security.getUnknownXMLObjects().add(binarySecurityToken);
        // Digital signature to verify the identity of the signer.
        
        
        Signature signature = createSAMLObject(Signature.class);
        Credential cred = keyManager.getCredential(keyAlias);
        signature.setSigningCredential(cred);
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        DocumentInternalIDContentReference timestampReference =
                new DocumentInternalIDContentReference(timestamp.getWSUId());
        timestampReference.getTransforms().add(CanonicalizationMethod.EXCLUSIVE);
        timestampReference.setDigestAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        signature.getContentReferences().add(timestampReference);
        
        DocumentInternalIDContentReference bodyReference =
                new DocumentInternalIDContentReference(WSSecurityHelper.getWSUId(body));
        bodyReference.getTransforms().add(CanonicalizationMethod.EXCLUSIVE);
        bodyReference.setDigestAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        signature.getContentReferences().add(bodyReference);
        KeyInfo keyInfo = createSAMLObject(KeyInfo.class);
        SecurityTokenReference securityTokenReference = createSAMLObject(SecurityTokenReference.class);
        keyInfo.getXMLObjects().add(securityTokenReference);
        
        
        X509Data x509Data = createSAMLObject(X509Data.class);
       
        x509Data.getX509IssuerSerials().add(KeyInfoHelper.buildX509IssuerSerial(
                publicCertificate.getIssuerX500Principal().getName(), publicCertificate.getSerialNumber()));
        securityTokenReference.getUnknownXMLObjects().add(x509Data);
       
        signature.setKeyInfo(keyInfo);
        security.getUnknownXMLObjects().add(signature);
        
        marshall(envelope);
        sign(signature);
        //securityModule.signObject(envelope, signature);
      
        // Build the W3C DOM representing the SOAP message.
        Element elem = marshall(envelope);
        
        //context.setOutboundSAMLMessage(null);
        //HTTPSOAP11Encoder encode = new HTTPSOAP11Encoder();
        //encode.encode(context);
        
        
        log.info(StaxUtils.toString(elem));
        
        //XMLObject result = send2(renewEndpointUrl, context, envelope);
        //NodeList lst = result.getDOM().getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion","Assertion");
        
        Envelope result = send(renewEndpointUrl, context, envelope);
        NodeList lst = result.getBody().getDOM().getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion","Assertion");
                         
        Node node = lst.item(0);
        log.debug("NODE: "+node.toString());
        
        StringWriter writer = new StringWriter();
        TransformerFactory factory = TransformerFactory.newInstance();       
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        
        String xml = writer.toString();
        
        return xml;
    }
    
    public static String encode(final X509Certificate certificate) throws CertificateEncodingException {
        final Base64.Encoder encoder = Base64.getEncoder();

        final byte[] rawCrtText = certificate.getEncoded();
        final String encodedCertText = new String(encoder.encode(rawCrtText));
        
        return encodedCertText;
    }
    
    public static void sign(Signature signature) {       
         
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
    
    public Element marshall(XMLObject object) {
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
    
    public XMLObject unmarshall(Element input) throws UnmarshallingException {
        Unmarshaller un = Configuration.getUnmarshallerFactory().getUnmarshaller(input);
        return un.unmarshall(input);
    }
       
    
    public static String randomId() {
        return secureRandomIdGenerator.generateIdentifier();
    }

   
    private Envelope send(String targetUrl, SAMLMessageContext context, Envelope envelope) throws SOAPException, CertificateEncodingException,
    MarshallingException, SignatureException, IllegalAccessException, org.opensaml.xml.security.SecurityException, URIException, MessageEncodingException {
           HttpClientBuilder clientBuilder = new HttpClientBuilder();
            //clientBuilder.setHttpsProtocolSocketFactory(SSLProtocolSocketFactory.getSocketFactory());
           CriteriaSet criteriaSet = new CriteriaSet();
           criteriaSet.add(new EntityIDCriteria(context.getPeerEntityId()));
           criteriaSet.add(new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS));
           criteriaSet.add(new UsageCriteria(UsageType.UNSPECIFIED));

           X509TrustManager trustManager = new X509TrustManager(criteriaSet, context.getLocalSSLTrustEngine());
           X509KeyManager manager = new X509KeyManager((X509Credential) keyManager.getCredential("hintls"));
           
           
           clientBuilder.setHttpsProtocolSocketFactory(
                   new TLSProtocolSocketFactory(manager, trustManager));
       
           
           HttpClient httpClient = clientBuilder.buildClient();
            httpClient.setHostConfiguration(getHostConfiguration(new URI(targetUrl, true, "UTF-8"), context, httpClient));
            
           
            HttpSOAPClient soapClient = new HttpSOAPClient(httpClient, new BasicParserPool());
                       
            BasicSOAPMessageContext soapContext = new BasicSOAPMessageContext();
            soapContext.setOutboundMessage(envelope);  
            log.info("ISSUER="+soapContext.getOutboundMessageIssuer());
            //soapContext.setOutboundMessageIssuer("https://test.ahdis.ch");
            log.info("SEND!");
            soapClient.send(targetUrl, soapContext);
            log.info("POST-SEND!");

            Envelope soapResponse = (Envelope)soapContext.getInboundMessage();
            
            return soapResponse;
    }
    
    protected HostConfiguration getHostConfiguration(URI uri, SAMLMessageContext context, HttpClient httpClient) throws MessageEncodingException {

        try {

            HostConfiguration hc = httpClient.getHostConfiguration();

            if (hc != null) {
                // Clone configuration from the HTTP Client object
                log.info("EXIST");
                hc = new HostConfiguration(hc);
            } else {
                // Create brand new configuration when there are no defaults
                log.info("NOT EXIST");
                hc = new HostConfiguration();
            }

            if (uri.getScheme().equalsIgnoreCase("http")) {

                log.info("Using HTTP configuration");
                hc.setHost(uri);

            } else {

                log.info("Using HTTPS configuration");
                log.info("PEER="+context.getPeerEntityId());
                CriteriaSet criteriaSet = new CriteriaSet();
                criteriaSet.add(new EntityIDCriteria(context.getPeerEntityId()));
                criteriaSet.add(new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS));
                criteriaSet.add(new UsageCriteria(UsageType.UNSPECIFIED));

                X509TrustManager trustManager = new X509TrustManager(criteriaSet, context.getLocalSSLTrustEngine());
                
                //X509KeyManager manager1 = new X509KeyManager(context.getLocalSSLCredential());                
                //log.info("TLS NOT NULL:"+keyManager.getCredential("hintls").toString());
               
                X509KeyManager manager = new X509KeyManager((X509Credential) keyManager.getCredential("hintls"));
                                
                HostnameVerifier hostnameVerifier = context.getLocalSSLHostnameVerifier();

                ProtocolSocketFactory socketFactory = getSSLSocketFactory(context, manager, trustManager, hostnameVerifier);
                Protocol protocol = new Protocol("https", socketFactory, 443);
                hc.setHost(uri.getHost(), uri.getPort(), protocol);
                               
                log.info("SET-HOST: "+uri.getHost()+" proto="+protocol.toString());
            }

            return hc;

        } catch (URIException e) {
            throw new MessageEncodingException("Error parsing remote location URI", e);
        }

    }

    /**
     * Method returns SecureProtocolSocketFactory used to connect to create SSL connections for artifact resolution.
     * By default we create instance of org.opensaml.ws.soap.client.http.TLSProtocolSocketFactory.
     *
     * @param context current SAML context
     * @param manager keys used for client authentication
     * @param trustManager trust manager for server verification
     * @param hostnameVerifier verifier for server hostname, or null
     * @return socket factory
     */
    protected SecureProtocolSocketFactory getSSLSocketFactory(SAMLMessageContext context, X509KeyManager manager, X509TrustManager trustManager, HostnameVerifier hostnameVerifier) {
        if (isHostnameVerificationSupported()) {
            return new TLSProtocolSocketFactory(manager, trustManager, hostnameVerifier);
        } else {
            return new TLSProtocolSocketFactory(manager, trustManager);
        }
    }

    /**
     * Check for the latest OpenSAML library. Support for HostnameVerification was added in openws-1.5.1 and
     * customers might use previous versions of OpenSAML.
     *
     * @return true when OpenSAML library support hostname verification
     */
    protected boolean isHostnameVerificationSupported() {
        try {
            TLSProtocolSocketFactory.class.getConstructor(javax.net.ssl.X509KeyManager.class, javax.net.ssl.X509TrustManager.class, javax.net.ssl.HostnameVerifier.class);
            return true;
        } catch (NoSuchMethodException e) {
            log.warn("HostnameVerification is not supported, update your OpenSAML libraries");
            return false;
        }
    }

}