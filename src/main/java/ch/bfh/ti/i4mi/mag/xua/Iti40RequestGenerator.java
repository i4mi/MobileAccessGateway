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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.config.IuaProperties;
import org.apache.camel.Body;
import org.apache.cxf.staxutils.StaxUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

import static org.opensaml.common.xml.SAMLConstants.SAML20_NS;
import static org.opensaml.common.xml.SAMLConstants.SAML20_PREFIX;

/**
 * Create a Get-X-User-Assertion SOAP Message from AssertionRequest Bean
 * @author alexander kreutz
 *
 */
@Slf4j
@Service
public class Iti40RequestGenerator {

	 @Value("${mag.iua.sp.endpoint-ref:#{null}}")
	 private String endpointReference;
	 
	 @Value("${mag.baseurl:}")
	 private String baseUrl;

	 @Autowired
	 private IuaProperties iuaProperties;
	 
	 private String BASE_MSG() { return "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">\n" + 
	 		"   <env:Header xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" + 
	 		"      <wsa:Action>http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue</wsa:Action>\n" + 
	 		"   </env:Header>\n" + 
	 		"   <env:Body>\n" + 
	 		"      <wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n" + 
	 		"         <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n" + 
	 		"         <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" + 
	 		"            <wsa:EndpointReference xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" + 
	 		"               <wsa:Address>"+(endpointReference != null ? endpointReference : baseUrl)+"</wsa:Address>\n" + 
	 		"            </wsa:EndpointReference>\n" + 
	 		"         </wsp:AppliesTo>\n" + 
	 		"         <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>\n" + 
	 		"         <wst:Claims Dialect=\"http://www.bag.admin.ch/epr/2017/annex/5/amendment/2\">\n" +
         	"         </wst:Claims>\n" + 
	 		"      </wst:RequestSecurityToken>\n" + 
	 		"   </env:Body>\n" + 
	 		"</env:Envelope>"; };
	 
	 public void addResourceId(SOAPElement claims, String id) throws SOAPException {
	   SOAPElement attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
	   attribute.setAttribute("Name", "urn:oasis:names:tc:xacml:2.0:resource:resource-id");
	   SOAPElement attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
	   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
	   attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
	   if (id!=null && !id.contains("^") && id.matches("\\d{18}")) {
		   id += "^^^&2.16.756.5.30.1.127.3.10.3&ISO";
	   }
	   attributeValue.setTextContent(id);
	 }
	 
	 public void addPurposeOfUse(SOAPElement claims, String purposeOfUse) throws SOAPException {
		   String purposeOfUseDisplay = "Normal Access";
		   if (purposeOfUse.equals("EMER")) purposeOfUseDisplay = "Emergency Access";
		   else if (purposeOfUse.equals("AUTO")) purposeOfUseDisplay = "Automatic Upload";
		 
		   SOAPElement attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
		   attribute.setAttribute("Name", "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
		   SOAPElement attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
		   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
		   attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:anyType");
		   SOAPElement purpose = attributeValue.addChildElement("PurposeOfUse", null, "urn:hl7-org:v3");
		   purpose.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "CE");
		   purpose.setAttribute("code", purposeOfUse);
		   purpose.setAttribute("codeSystem", "2.16.756.5.30.1.127.3.10.5");
		   purpose.setAttribute("codeSystemName", "eHealth Suisse Verwendungszweck");
		   purpose.setAttribute("displayName", purposeOfUseDisplay);
		  		 
	}
	 
	 public void addRole(SOAPElement claims, String role) throws SOAPException {
		 
		   String displayName = "";
		   switch (role) {
		   case "PAT":displayName = "Patient";break;
		   case "HCP":displayName = "Healthcare professional";break;
		   case "ASS":displayName = "Assistant";break;
		   case "REP":displayName = "Representative";break;
		   case "TCU":displayName = "Technical user";break; 
		   case "DADM":displayName = "Document Administrator";break;
		   case "PADM":displayName = "Policy Administrator";break;
		   }		 
		 
		   SOAPElement attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
		   attribute.setAttribute("Name", "urn:oasis:names:tc:xacml:2.0:subject:role");
		   attribute.setAttribute("NameFormat", "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
		   SOAPElement attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
		   		   
		   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
		   
		   SOAPElement roleElement = attributeValue.addChildElement("Role", null, "urn:hl7-org:v3");
		   roleElement.setAttribute("code", role);
		   roleElement.setAttribute("codeSystem", "2.16.756.5.30.1.127.3.10.6");
		   roleElement.setAttribute("codeSystemName", "eHealth Suisse EPR Akteure");
		   roleElement.setAttribute("displayName", displayName);
		   roleElement.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "CE");		   		  
	}
	 
	 public void addPrincipal(SOAPElement claims, String principalId, String principalName) throws SOAPException {
		   SOAPElement attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
		   attribute.setAttribute("Name", "urn:e-health-suisse:principal-id");		   
		   SOAPElement attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
		   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
		   attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
	       attributeValue.setTextContent(principalId);		   		  		   		 	   		 
	       
	       attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
		   attribute.setAttribute("Name", "urn:e-health-suisse:principal-name");		   
		   attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
		   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
		   attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
	       attributeValue.setTextContent(principalName);
	}
	 
	 public void addOrganization(SOAPElement claims, List<String> orgIds, List<String> orgNames) throws SOAPException {
		 if (orgIds!=null) {
			 for (String orgId : orgIds) {
			   SOAPElement attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
			   attribute.setAttribute("Name", "\"urn:oasis:names:tc:xspa:1.0:sub-ject:organization-id");		   
			   SOAPElement attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
			   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
			   attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
		       attributeValue.setTextContent(orgId);
			 }
		 }
		 if (orgNames != null) {
			 for (String orgName : orgNames) {
			   SOAPElement attribute = claims.addChildElement("Attribute", SAML20_PREFIX, SAML20_NS);
			   attribute.setAttribute("Name", "urn:oasis:names:tc:xspa:1.0:sub-ject:organization");		   
			   SOAPElement attributeValue = attribute.addChildElement("AttributeValue", SAML20_PREFIX, SAML20_NS);
			   attributeValue.addNamespaceDeclaration("xs", "http://www.w3.org/2001/XMLSchema");
			   attributeValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
		       attributeValue.setTextContent(orgName);
			 }
		 }
	}
	
	
	 public Element addSecurityHeader(String input) throws XMLStreamException {
		 return StaxUtils.read(new StringReader(input)).getDocumentElement();
	 }
	
	 public SOAPMessage buildAssertion(@Body AssertionRequest request) throws SOAPException, IOException, XMLStreamException, AuthException {
		 Object token = request.getSamlToken();
		 if (token == null) {
			 throw new AuthException(400, "server_error", "No SAML token found");
		 }
		 log.debug("IDP token is an instance of: {}", token.getClass().getSimpleName());
		 /*if (token.startsWith("IHE-SAML ")) token = token.substring("IHE-SAML ".length());			
		 byte[] decoded = Base64.getDecoder().decode(token);
		 token = new String(decoded);*/
		 if (token instanceof String && token.toString().startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
			 token = token.toString().substring("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".length());
			 log.debug("Decoded IDP Token: {}", token);
		 } else if (token instanceof Element) {
			 log.debug("Decoded IDP Token: {}", StaxUtils.toString((Element) token));
		 }
		 
		 MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
		 SOAPMessage message = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(BASE_MSG().getBytes(StandardCharsets.UTF_8)));

		 final Element idpAssertionElement;
		 message.getSOAPHeader().addChildElement("MessageID","wsa","http://www.w3.org/2005/08/addressing").addTextNode(UUID.randomUUID().toString());
		 if (token instanceof String) {
			 idpAssertionElement = addSecurityHeader(token.toString());
		 } else {
			 idpAssertionElement = (Element) token;
		 }
		 final Node node = message.getSOAPHeader().getOwnerDocument().importNode(idpAssertionElement, true);
		 message.getSOAPHeader().addChildElement("Security", "wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd").appendChild(node);

		 if (request.getResourceId() == null) {
			 // Check if the IDP token contains an email address
			 final String email = XuaUtils.getEmailFromIdpToken(idpAssertionElement);
			 log.debug("resourceId is null, extracted email in IDP token: {}", email);
			 final var emailMap = this.iuaProperties.getPatients();
			 if (emailMap != null && emailMap.containsKey(email)) {
				 request.setResourceId(emailMap.get(email));
				 log.debug("Using resource ID from email mapping: {}", request.getResourceId());
			 } else {
				 log.info("No resource ID provided and no email mapping found for: {}", email);
			 }
		 }

		 if (request.getResourceId() == null) {
			 log.info("No resource ID provided in request and no email mapping found, can't create Get-X-User-Assertion request");
			 throw new AuthException(400, "invalid_request", "No resource ID provided");
		 }

		 SOAPElement claims = (SOAPElement) message.getSOAPBody().getElementsByTagNameNS("http://docs.oasis-open.org/ws-sx/ws-trust/200512", "Claims").item(0);
		 if (request.getResourceId() != null) {
		     addResourceId(claims, request.getResourceId()); // format "761337610435200998^^^&2.16.756.5.30.1.127.3.10.3&ISO"
		 }
		 
		 if (request.getPurposeOfUse() != null) {
			 addPurposeOfUse(claims, request.getPurposeOfUse());
		 }
		 
		 if (request.getRole() != null) {
			addRole(claims, request.getRole()); 
		 }
		 
		 if (request.getPrincipalID() != null || request.getPrincipalName() != null) {
			 addPrincipal(claims, request.getPrincipalID(), request.getPrincipalName());
		 }
		 
		 if (request.getOrganizationID() != null || request.getOrganizationName() != null) {
			 addOrganization(claims, request.getOrganizationID(), request.getOrganizationName());
		 }
		 		 
		 log.debug("Sending Assertion Request: "+message.toString());
		 
		 message.saveChanges();
		 				 
		 return message;
	 }
	 	 
}
