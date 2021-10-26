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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.openehealth.ipf.commons.ihe.fhir.translation.FhirTranslator;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.pmir.BasePMIRResponseConverter;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.mccimt000300UV01.MCCIMT000300UV01Acknowledgement;
import net.ihe.gazelle.hl7v3.mccimt000300UV01.MCCIMT000300UV01AcknowledgementDetail;
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
public class Iti83ResponseConverter extends BasePMIRResponseConverter implements ToFhirTranslator<byte[]> {

	@Autowired
	PatientReferenceCreator patientRefCreator;

	@Autowired
	private Config config;

	public Parameters translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			// FIX for xmlns:xmlns
	    String content = new String(input);
	    content = content.replace("xmlns:xmlns","xmlns:xxxxx");
		PRPAIN201310UV02Type msg = HL7V3Transformer.unmarshallMessage(PRPAIN201310UV02Type.class, new ByteArrayInputStream(content.getBytes()));
		
		Parameters query = (Parameters) parameters.get(Utils.KEPT_BODY);
		List<Type> targetSystemList = (List<Type>) query.getParameters("targetSystem");	
		Parameters response = new Parameters();
		
		PRPAIN201310UV02MFMIMT700711UV01ControlActProcess controlAct = msg.getControlActProcess();
		List<MCCIMT000300UV01Acknowledgement> acks = msg.getAcknowledgement();
		String errtext = "";
		for (MCCIMT000300UV01Acknowledgement ack : acks) {
			for (MCCIMT000300UV01AcknowledgementDetail ackDetail : ack.getAcknowledgementDetail()) {
				if (ackDetail.getText() != null) errtext+=toText(ackDetail.getText());
			}
		}
		// OK NF AE
		String queryResponseCode = controlAct.getQueryAck().getQueryResponseCode().getCode();
		if ("NF".equals(queryResponseCode)) {			
			throw new ResourceNotFoundException("sourceIdentifier Patient Identifier not found", error(IssueType.NOTFOUND, errtext.length()>0 ? errtext : "sourceIdentifier Patient Identifier not found"));
		}
		if ("AE".equals(queryResponseCode)) {
			throw new InvalidRequestException("sourceIdentifier Assigning Authority not found", error(IssueType.INVALID, errtext.length()>0 ? errtext : "sourceIdentifier Assigning Authority not found"));
		}
		
		Set<String> acceptedTargetSystem = new HashSet<String>();
		Set<String> noDuplicates = new HashSet<String>();
		if (targetSystemList != null) {
			for (Type targetSystemType : targetSystemList) {
				UriType targetSystem = (UriType) targetSystemType;
				acceptedTargetSystem.add(targetSystem.getValue());
			}
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
				
				if (!noDuplicates.contains(root) && (acceptedTargetSystem.isEmpty() || acceptedTargetSystem.contains("urn:oid:"+root))) {
				  response.addParameter().setName("targetIdentifier").setValue((new Identifier()).setSystem("urn:oid:"+root).setValue(extension));
				  noDuplicates.add(root);
				}
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
