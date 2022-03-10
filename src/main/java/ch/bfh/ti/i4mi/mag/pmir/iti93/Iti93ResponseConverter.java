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
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.pmir.BasePMIRResponseConverter;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.mcciin000002UV01.MCCIIN000002UV01Type;
import net.ihe.gazelle.hl7v3.mccimt000200UV01.MCCIMT000200UV01AcknowledgementDetail;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * ITI-93 from ITI-44 response converter
 * @author alexander kreutz
 *
 */
@Component
public class Iti93ResponseConverter extends BasePMIRResponseConverter implements ToFhirTranslator<byte[]> {

	@Autowired
	private Config config;
	
	/**
	 * translate ITI-44 response to ITI-93 response
	 */
	public Object translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			Patient request = (Patient) parameters.get(Utils.KEPT_BODY);
			
			// FIX for xmlns:xmlns
			String content = new String(input);
		    content = content.replace("xmlns:xmlns","xmlns:xxxxx");
			
			MCCIIN000002UV01Type  msg = HL7V3Transformer.unmarshallMessage(MCCIIN000002UV01Type.class, new ByteArrayInputStream(content.getBytes()));
									
			
			for (net.ihe.gazelle.hl7v3.mccimt000200UV01.MCCIMT000200UV01Acknowledgement akk : msg.getAcknowledgement()) {
				CS code = akk.getTypeCode();
				if (!code.getCode().equals("AA") && !code.getCode().equals("CA")) {
					OperationOutcome outcome = new OperationOutcome();
					for (MCCIMT000200UV01AcknowledgementDetail detail : akk.getAcknowledgementDetail()) {
						OperationOutcomeIssueComponent issue = outcome.addIssue();
						issue.setDetails(new CodeableConcept().setText(toText(detail.getText())).addCoding(transform(detail.getCode())));
					}
					return new MethodOutcome(outcome).setCreated(false);
				}
			}
			return request; // FIXME patietn
		} catch (JAXBException e) {
			throw new InvalidRequestException("failed parsing response");
		}
	}
	
	public Coding transform(CE code) {
		return new Coding(code.getCodeSystem(),code.getCode(),code.getDisplayName());
	}
	
	
}
