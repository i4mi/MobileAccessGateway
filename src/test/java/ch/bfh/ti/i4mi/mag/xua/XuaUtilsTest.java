package ch.bfh.ti.i4mi.mag.xua;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class XuaUtilsTest {

    @Test
    void testGetEmailFromIdpToken() throws Exception {
        final var docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        final var docBuilder = docBuilderFactory.newDocumentBuilder();

        final Element assertion = docBuilder.parse(new ByteArrayInputStream("""
    <saml:Assertion ID="ID_5619301b-3b16-4c80-82a6-80a88f42c052" IssueInstant="2025-05-28T05:56:13.559Z" Version="2.0" xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">
    	<saml:Issuer>https://timtest.geneveid.ch/auth/realms/dep-geneve-test</saml:Issuer>
    	<dsig:Signature xmlns:dsig="http://www.w3.org/2000/09/xmldsig#">
    		<dsig:SignedInfo>
    			<dsig:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
    			<dsig:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
    			<dsig:Reference URI="#ID_5619301b-3b16-4c80-82a6-80a88f42c052">
    				<dsig:Transforms>
    					<dsig:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
    					<dsig:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
    				</dsig:Transforms>
    				<dsig:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
    				<dsig:DigestValue>EIfAZ/JU3alw3Mz0/tcL082S3OfgXCGYSR+oLOafQFI=</dsig:DigestValue>
    			</dsig:Reference>
    		</dsig:SignedInfo>
    		<dsig:SignatureValue>TVZDmrqTn/qYm5Y7xf/xdJJssr2AxwcxhH86yjtcLYHgtstXjuWiad0KdALsHBnuNT077sMZ7679ApWTMOmz98ew1xbb7C7kIJO+bBy2m2xsHEBIU+WeLbvXDWbFE6ku1IPmPlzLuWvt/KwyT8rXPuBOvK4A7gyvIVoX9q5mWdSuhTdF2YI9YP5AzH57YNwz7/jiZyz2bw8of7gn5hCjBF6ccHYdBSOEoo+ltktX6/mTRqoH+axGjV9bMilbk5vhAKlpFM6eaooqEuWExnyJucLt4EYfezMg7KuzScDuJT/1bq7SKCiCcT43qFVMskg0TieGggRFEdew/VIE3X+QRw==</dsig:SignatureValue>
    		<dsig:KeyInfo>
    			<dsig:KeyName>h5xLf-dJunneckfg4RgyNB7p-eX6sAbdNXlLx4-fXwQ</dsig:KeyName>
    			<dsig:X509Data>
    				<dsig:X509Certificate>MIICrTCCAZUCBgFyx5NZuDANBgkqhkiG9w0BAQsFADAaMRgwFgYDVQQDDA9kZXAtZ2VuZXZlLXRlc3QwHhcNMjAwNjE4MTMxNTIyWhcNMzAwNjE4MTMxNzAyWjAaMRgwFgYDVQQDDA9kZXAtZ2VuZXZlLXRlc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCI0ZU7cGOTgoZ+FztRfyzN+jkYdVSNdqbKYeZD/MHrqipwjIIX5JdqxlYvXo5itKuxsg7qSVTCaSSjFA21I3nOtkbz8V4BpHNzb4j79c0ldPk6JZBs+0oMlK8q6/r6UT5f3druLJkcuyv5kMdJD+QYiyCuopbaNiwXAVuQw4TpCA9ZNeOkimEEnF8loNJDptG7J/lQ89WyHrOF+3qXiAXrO2xfKahqH+wzHeEXtGZeLV1h1IpoeslzQKBeluk1IcOM6JY15Vze0ezlIOJwWdF+Aopjh9bVIfOqPgruw61veJjWQyeQxEPPcquNUJUKP2zzxEdPLAjrOFTmw6Fol2yDAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAGyg2QPirKzLeYG2NC8fRJmMgqdwmDmmEtmB3RICAz66TciJ8eJC3R05dq8WgUalZqNykm0Ta6427zw5ZD+YWREkWZYRtd73+V4AjIwpC9l/uy3vXVxmsIOwfalOV5GmZ9FUPpNlKvszpeJV5hgixD/OUj615gJsI1l0ToQBt8BxyTcxDcbRwTZvNtZsz2rzjBcgZUgQRfWUSawI1gHpFax9nIlzQCqUtHPc4Hyo7vX1uZ+9IcKW4xoVQ9UrA8qG+GdGgI7+yUPpWb26Lq+B2Nimd7bS/dHkCUy3Hp+FuZnweOTCf3bMNzf2tkakSvHJWSkqPcLxI3ATu4yABEH7fJc=</dsig:X509Certificate>
    			</dsig:X509Data>
    			<dsig:KeyValue>
    				<dsig:RSAKeyValue>
    					<dsig:Modulus>iNGVO3Bjk4KGfhc7UX8szfo5GHVUjXamymHmQ/zB66oqcIyCF+SXasZWL16OYrSrsbIO6klUwmkkoxQNtSN5zrZG8/FeAaRzc2+I+/XNJXT5OiWQbPtKDJSvKuv6+lE+X93a7iyZHLsr+ZDHSQ/kGIsgrqKW2jYsFwFbkMOE6QgPWTXjpIphBJxfJaDSQ6bRuyf5UPPVsh6zhft6l4gF6ztsXymoah/sMx3hF7RmXi1dYdSKaHrJc0CgXpbpNSHDjOiWNeVc3tHs5SDicFnRfgKKY4fW1SHzqj4K7sOtb3iY1kMnkMRDz3KrjVCVCj9s88RHTywI6zhU5sOhaJdsgw==</dsig:Modulus>
    					<dsig:Exponent>AQAB</dsig:Exponent>
    				</dsig:RSAKeyValue>
    			</dsig:KeyValue>
    		</dsig:KeyInfo>
    	</dsig:Signature>
    	<saml:Subject>
    		<saml:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent">G-20d1a52d-f093-4043-9de9-a8e5755df61e</saml:NameID>
    		<saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
    			<saml:SubjectConfirmationData InResponseTo="a28ffdc21fi8fjbgaa4g693989gha1" NotOnOrAfter="2025-05-28T06:01:11.559Z" Recipient="https://test.ahdis.ch/eprik-cara/saml/SSO/alias/geneveid"/>
    		</saml:SubjectConfirmation>
    	</saml:Subject>
    	<saml:Conditions NotBefore="2025-05-28T05:56:11.559Z" NotOnOrAfter="2025-05-28T06:01:11.559Z">
    		<saml:AudienceRestriction>
    			<saml:Audience>https://test.ahdis.ch/eprik-cara/saml/SSO/alias/geneveid</saml:Audience>
    		</saml:AudienceRestriction>
    	</saml:Conditions>
    	<saml:AuthnStatement AuthnInstant="2025-05-28T05:56:13.559Z" SessionIndex="f3ced2ea-fe83-4a81-87ee-b606952ecf62::57b3e142-d933-4207-ad20-3630205e7ecc" SessionNotOnOrAfter="2025-05-28T15:56:13.559Z">
    		<saml:AuthnContext>
    			<saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml:AuthnContextClassRef>
    		</saml:AuthnContext>
    	</saml:AuthnStatement>
    	<saml:AttributeStatement>
    		<saml:Attribute Name="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:uri">
    			<saml:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">quentin.ligier@ahdis.ch</saml:AttributeValue>
    		</saml:Attribute>
    		<saml:Attribute FriendlyName="identno" Name="identno" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
    			<saml:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">D1234567</saml:AttributeValue>
    		</saml:Attribute>
    		<saml:Attribute FriendlyName="dateofbirth" Name="dateofbirth" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
    			<saml:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">01.01.2000</saml:AttributeValue>
    		</saml:Attribute>
    		<saml:Attribute FriendlyName="gender" Name="gender" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
    			<saml:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">M</saml:AttributeValue>
    		</saml:Attribute>
    		<saml:Attribute FriendlyName="firstName" Name="firstname" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
    			<saml:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">Quentin</saml:AttributeValue>
    		</saml:Attribute>
    		<saml:Attribute FriendlyName="lastName" Name="familyname" NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic">
    			<saml:AttributeValue xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">Ligier</saml:AttributeValue>
    		</saml:Attribute>
    	</saml:AttributeStatement>
    </saml:Assertion>""".getBytes(StandardCharsets.UTF_8))).getDocumentElement();

        assertNotNull(assertion);

        final String email = XuaUtils.getEmailFromIdpToken(assertion);
        assertEquals("quentin.ligier@ahdis.ch", email);
    }
}