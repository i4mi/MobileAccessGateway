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

			Map<String, List<String>> httpHeaders = (Map<String, List<String>>) exchange.getMessage().getHeader("FhirHttpHeaders");

			String converted = null;

			if (httpHeaders != null) {
				List<String> header = httpHeaders.get("Authorization");
				if (header != null) {
					converted = convert(header.get(0));
					httpHeaders.remove("Authorization");
				}
			} else {
				Object authHeader = exchange.getMessage().getHeader("Authorization");
				if (authHeader != null) {
					exchange.getMessage().removeHeader("Authorization");
					converted = convert(authHeader.toString());
				}
			}
			if (converted != null) {

				
               converted = "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ID=\"_b409ce75-9fc0-4682-aef0-8c4ddb00dd47\" IssueInstant=\"2020-09-24T06:49:10.501Z\" Version=\"2.0\"><saml2:Issuer>http://ith-icoserve.com/eHealthSolutionsSTS</saml2:Issuer><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" + 
               		"<ds:SignedInfo>\n" + 
               		"<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" + 
               		"<ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" + 
               		"<ds:Reference URI=\"#_b409ce75-9fc0-4682-aef0-8c4ddb00dd47\">\n" + 
               		"<ds:Transforms>\n" + 
               		"<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" + 
               		"<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"><ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xsd\"/></ds:Transform>\n" + 
               		"</ds:Transforms>\n" + 
               		"<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>\n" + 
               		"<ds:DigestValue>yhX0X8DNom9Oa2g0YVkSM0P+fFxpVxZ2oxQ/cx8Lm+U=</ds:DigestValue>\n" + 
               		"</ds:Reference>\n" + 
               		"</ds:SignedInfo>\n" + 
               		"<ds:SignatureValue>\n" + 
               		"TtP8EsoNPiP2ecm+S+Zf1N8SUPEiTNLub1xUp7ajA06OV2zkMiL4f1WgDBKvjB+M3QDFdYGY7tUJ\n" + 
               		"bD3xKHiqXO1k1CN3/qYLc5ioT0GquR4gIxUAx+6BXQDtYqVaG6A1Mpu8gggCPrkaFly3eUPOGeCc\n" + 
               		"0pdvoFI/hTKfx2Z3FXNnQEoGWQBI88bMrEdvZLsT5OG19qWozIIzkLm7HRzyJb1bv+c0iWezJuwY\n" + 
               		"xj/+k0idUziwDeoVbZHzwY9P6jW9zC3NUgZ6byKCtHi6zB7SwqGQ1zkaDcnPMjBQZeBYuauzoqS7\n" + 
               		"NCqG1E8EA8lQ5wYd5NbU4ijUvT2rystMaAXmag==\n" + 
               		"</ds:SignatureValue>\n" + 
               		"<ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIID5jCCA0+gAwIBAgICANYwDQYJKoZIhvcNAQENBQAwRTELMAkGA1UEBhMCQ0gxDDAKBgNVBAoM\n" + 
               		"A0lIRTEoMCYGA1UEAwwfZWhlYWx0aHN1aXNzZS5paGUtZXVyb3BlLm5ldCBDQTAeFw0xOTExMTkw\n" + 
               		"NjE3MDBaFw0yOTExMTkwNjE3MDBaMDIxCzAJBgNVBAYTAkFUMRUwEwYDVQQKDAxJVEggaWNvc2Vy\n" + 
               		"dmUxDDAKBgNVBAMMA1NUUzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANc8txP/o03W\n" + 
               		"4+5FAzBsHGjT+2gKaWX4HgrMNWEmpavTaYPe8v7T8jYqvZbOHqdEYsZt4IflQSORpr9CM/Cbrd7y\n" + 
               		"WzwQ3fTs5bmIsFedAiRxSMli5d/7oBx0cwKv31OHzrB39PfkQstBKAm05qapY6H2mnf6yyWKCjbF\n" + 
               		"++YdQcsUVoTAAMwAIFFseDQEarCGAoD5Nd7rCeqasMVvTFobMxGhqntgiGlJbY+hNFyxz6dfVFtf\n" + 
               		"sTlHDdfqsak9Coj6DrSharBNHOg6RRiNvc3vJ/xUDWPtS+wjddz2hWiauh5xB+VA+cSu+QkCeM2c\n" + 
               		"f2yKV0FCV91dqSw2//sXCaaEP/8CAwEAAaOCAXIwggFuMEIGA1UdHwQ7MDkwN6A1oDOGMWh0dHBz\n" + 
               		"Oi8vY3RzLXBvc3Qua2VyZXZhbC5jb20vZ3NzL2NybC8yMi9jYWNybC5jcmwwQAYJYIZIAYb4QgEE\n" + 
               		"BDMWMWh0dHBzOi8vY3RzLXBvc3Qua2VyZXZhbC5jb20vZ3NzL2NybC8yMi9jYWNybC5jcmwwQAYJ\n" + 
               		"YIZIAYb4QgEDBDMWMWh0dHBzOi8vY3RzLXBvc3Qua2VyZXZhbC5jb20vZ3NzL2NybC8yMi9jYWNy\n" + 
               		"bC5jcmwwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUOZlov/TXB6YHUyMV1yWK2qjG0BswHwYDVR0j\n" + 
               		"BBgwFoAUKJfv3d4xWGxW8oZG4hHkPjhxXy8wDgYDVR0PAQH/BAQDAgTwMBEGCWCGSAGG+EIBAQQE\n" + 
               		"AwIF4DAzBgNVHSUELDAqBggrBgEFBQcDAgYIKwYBBQUHAwQGCisGAQQBgjcUAgIGCCsGAQUFBwMB\n" + 
               		"MA0GCSqGSIb3DQEBDQUAA4GBAIVAG7X24vwje17fcDshobEQ6SOV43JYYTUx5bfB6PRbiJOb8Kv9\n" + 
               		"2Ijs9o9JAuol1tisQ0LZX/ddEqZ02i3gw4HsC5PSX3pTvndbhRxC6qL9fGu8x2FJ2W3Os/au5WNP\n" + 
               		"VnLkX60dKe+2aMydlWxKAJiGZE10ppMXOV9GlVIiHysp</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\" NameQualifier=\"urn:gs1:gln\">7601002469191</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData InResponseTo=\"_27b0268d272c0d71593bfdaeb6675727\" NotOnOrAfter=\"2020-09-26T06:49:10.501Z\" Recipient=\"agfa_orbis\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2020-09-24T06:49:09.501Z\" NotOnOrAfter=\"2020-09-26T06:49:10.501Z\"><saml2:AudienceRestriction><saml2:Audience>urn:e-health-suisse:token-audience:all-communities</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AuthnStatement AuthnInstant=\"2020-09-24T06:49:10.501Z\"><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement><saml2:AttributeStatement><saml2:Attribute Name=\"urn:oasis:names:tc:xspa:1.0:subject:organization\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">Spital Y</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"urn:oasis:names:tc:xspa:1.0:subject:organization-id\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:anyURI\">urn:oid:2.16.10.89.211</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"urn:oasis:names:tc:xspa:1.0:subject:subject-id\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">Ann Andrews</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"urn:oasis:names:tc:xacml:2.0:subject:role\"><saml2:AttributeValue><Role xmlns=\"urn:hl7-org:v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" code=\"HCP\" codeSystem=\"2.16.756.5.30.1.127.3.10.6\" codeSystemName=\"eHealth Suisse EPR Actors\" displayName=\"Healthcare professional\" xsi:type=\"CE\"/></saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"urn:oasis:names:tc:xspa:1.0:subject:purposeofuse\"><saml2:AttributeValue><PurposeOfUse xmlns=\"urn:hl7-org:v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" code=\"NORM\" codeSystem=\"2.16.756.5.30.1.127.3.10.5\" codeSystemName=\"EprPurposeOfUse\" displayName=\"Normal Access\" xsi:type=\"CE\"/></saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"urn:oasis:names:tc:xacml:2.0:resource:resource-id\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">761337610411353650^^^&amp;2.16.756.5.30.1.127.3.10.3&amp;ISO</saml2:AttributeValue></saml2:Attribute><saml2:Attribute Name=\"urn:ihe:iti:xca:2010:homeCommunityId\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:anyURI\">urn:oid:1.3.6.1.4.1.21367.2017.2.6.19</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>\n" + 
               		"";
								
				converted = "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"+converted+"</wsse:Security>";

				System.out.println(converted);
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
