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

package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.r4.model.MessageHeader.MessageSourceComponent;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.ED;
import net.ihe.gazelle.hl7v3.mcciin000002UV01.MCCIIN000002UV01Type;
import net.ihe.gazelle.hl7v3.mccimt000200UV01.MCCIMT000200UV01AcknowledgementDetail;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01ControlActProcess;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * ITI-93 from ITI-44 response converter
 * @author alexander kreutz
 *
 */
@Component
public class Iti93ResponseConverter implements ToFhirTranslator<byte[]> {

	@Autowired
	private Config config;
	
	/**
	 * translate ITI-44 response to ITI-93 response
	 */
	public Bundle translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			System.out.println(new String(input));
			
			// FIX for xmlns:xmlns
			String content = new String(input);
		    content = content.replace("xmlns:xmlns","xmlns:xxxxx");
			
			MCCIIN000002UV01Type  msg = HL7V3Transformer.unmarshallMessage(MCCIIN000002UV01Type.class, new ByteArrayInputStream(content.getBytes()));
									
			Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.MESSAGE);
                   				
			Bundle requestBundle = (Bundle) parameters.get(Utils.KEPT_BODY);
			MessageHeader header = (MessageHeader) requestBundle.getEntryFirstRep().getResource();
			
			MessageSourceComponent source = new MessageSourceComponent();
			source.setEndpoint(config.getBaseurl());
			header.setSource(source );
			
			MessageHeaderResponseComponent response = new MessageHeaderResponseComponent();
			
			response.setCode(ResponseType.OK);
			for (net.ihe.gazelle.hl7v3.mccimt000200UV01.MCCIMT000200UV01Acknowledgement akk : msg.getAcknowledgement()) {
				CS code = akk.getTypeCode();
				if (!code.getCode().equals("AA") && !code.getCode().equals("CA")) {
					response.setCode(ResponseType.FATALERROR);
					OperationOutcome outcome = new OperationOutcome();
					response.setDetails((Reference) new Reference().setResource(outcome));
					for (MCCIMT000200UV01AcknowledgementDetail detail : akk.getAcknowledgementDetail()) {
						OperationOutcomeIssueComponent issue = outcome.addIssue();
						issue.setDetails(new CodeableConcept().setText(toText(detail.getText())).addCoding(transform(detail.getCode())));
					     	
					}
				}
			}
			
			
			response.setIdentifier(header.getId());
			header.setId((String) null);
			header.setResponse(response );
			header.setFocus(null);
			
			BundleEntryComponent cmp = responseBundle.addEntry();
			cmp.setResource(header);			
			
			return responseBundle;
		} catch (JAXBException e) {
			throw new InvalidRequestException("failed parsing response");
		}
	}
	
	public Coding transform(CE code) {
		return new Coding(code.getCodeSystem(),code.getCode(),code.getDisplayName());
	}
	
	public String toText(ED in) {
		StringBuffer result = new StringBuffer();
		for (java.io.Serializable obj : in.getMixed()) {
			if (obj instanceof String) {
				result.append((String) obj);
			}
		}
		return result.toString();
	}
}
