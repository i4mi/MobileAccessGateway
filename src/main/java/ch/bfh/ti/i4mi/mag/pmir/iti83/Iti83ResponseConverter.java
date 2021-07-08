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

package ch.bfh.ti.i4mi.mag.pmir.iti83;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.openehealth.ipf.commons.ihe.fhir.translation.FhirTranslator;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02Type;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * ITI-83 from ITI-45 response converter
 * @author alexander kreutz
 *
 */
@Component
public class Iti83ResponseConverter implements ToFhirTranslator<byte[]> {

	@Autowired
	PatientReferenceCreator patientRefCreator;

	@Autowired
	private Config config;

	public OperationOutcome error(IssueType type, String diagnostics) {
		OperationOutcome result = new OperationOutcome();
		
		OperationOutcomeIssueComponent issue = result.addIssue();
		issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
		issue.setCode(type);
		issue.setDiagnostics(diagnostics);
		return result;
	}
	
	public Parameters translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			// FIX for xmlns:xmlns
	    String content = new String(input);
	    content = content.replace("xmlns:xmlns","xmlns:xxxxx");
		PRPAIN201310UV02Type msg = HL7V3Transformer.unmarshallMessage(PRPAIN201310UV02Type.class, new ByteArrayInputStream(content.getBytes()));
		
		Parameters response = new Parameters();
		
		PRPAIN201310UV02MFMIMT700711UV01ControlActProcess controlAct = msg.getControlActProcess();
				
		// OK NF AE
		String queryResponseCode = controlAct.getQueryAck().getQueryResponseCode().getCode();
		if ("NF".equals(queryResponseCode)) {			
			throw new ResourceNotFoundException("sourceIdentifier Patient Identifier not found", error(IssueType.NOTFOUND, "sourceIdentifier Patient Identifier not found"));
		}
		if ("AE".equals(queryResponseCode)) {
			throw new InvalidRequestException("sourceIdentifier Assigning Authority not found", error(IssueType.INVALID, "sourceIdentifier Assigning Authority not found"));
		}
		
		List<PRPAIN201310UV02MFMIMT700711UV01Subject1> subjects = controlAct.getSubject();
		for (PRPAIN201310UV02MFMIMT700711UV01Subject1 subject : subjects) {
			boolean targetIdAdded = false;
			
			List<II> ids = new ArrayList<II>();
			
			ids.addAll(subject.getRegistrationEvent().getSubject1().getPatient().getId());
			for (var otherId : subject.getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getAsOtherIDs()) {
				ids.addAll(otherId.getId());
			}
						
			for (II ii : ids) {
				String root = ii.getRoot();
				String extension = ii.getExtension();
				response.addParameter().setName("targetIdentifier").setValue((new Identifier()).setSystem("urn:oid:"+root).setValue(extension));
				if (!targetIdAdded && root.equals(config.getOidMpiPid())) {
					response.addParameter().setName("targetId").setValue(patientRefCreator.createPatientReference(root, extension));
					targetIdAdded = true;
				}
			}
			
			
		}
						
		return response;
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new InvalidRequestException("failed parsing response");
		}
	}
}
