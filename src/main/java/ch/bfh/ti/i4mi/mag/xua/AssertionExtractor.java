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

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.XMLConstants;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.camel.Body;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * Extract a SAML Assertion from a SOAP Message and return it as String
 * @author alexander
 *
 */
@Slf4j
public class AssertionExtractor {

	public String handle(@Body SOAPMessage in) throws SOAPException, TransformerConfigurationException, TransformerException, UnsupportedEncodingException {
		log.debug("INPUT:"+in);
		SOAPBody body = in.getSOAPBody();
		NodeList lst = body.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion","Assertion");
		Node node = lst.item(0);
		log.debug("NODE: "+node.toString());
		
		StringWriter writer = new StringWriter();
		TransformerFactory factory = TransformerFactory.newInstance();
		//factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
		//factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "all");
		
		Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		
		String xml = writer.toString();
		log.debug("TOKEN-PART:"+xml);
		
		return xml;		
				
	}
}
