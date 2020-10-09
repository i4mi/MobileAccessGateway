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

package ch.bfh.ti.i4mi.mag.pmir.iti78;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Address.AddressUse;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import net.ihe.gazelle.hl7v3.coctmt030007UV.COCTMT030007UVPerson;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpStreetAddressLine;
import net.ihe.gazelle.hl7v3.datatypes.BL;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EN;
import net.ihe.gazelle.hl7v3.datatypes.ENXP;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.EnPrefix;
import net.ihe.gazelle.hl7v3.datatypes.EnSuffix;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.INT;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.ST;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02MFMIMT700711UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02MFMIMT700711UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02MFMIMT700711UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02MFMIMT700711UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201306UV02.PRPAIN201306UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02LanguageCommunication;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201310UV02.PRPAMT201310UV02PersonalRelationship;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * Convert to ITI-43 response back to ITI-78 response
 * @author alexander kreutz
 *
 */
@Component
public class Iti78ResponseConverter implements ToFhirTranslator<byte[]> {

	@Autowired
	PatientReferenceCreator patientRefCreator;
	
	public OperationOutcome error(IssueType type, String diagnostics) {
		OperationOutcome result = new OperationOutcome();
		
		OperationOutcomeIssueComponent issue = result.addIssue();
		issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
		issue.setCode(type);
		issue.setDiagnostics(diagnostics);
		return result;
	}
	
	public void errorFromException(Exception e) {
		throw new UnclassifiedServerFailureException(500, e.getMessage());		
	}
	
	public String val(ST in) {
		return in.getListStringValues().get(0);
	}
	
	public String val(List<? extends ST> in) {
		if (in == null || in.isEmpty()) return null;
		String result = null;
		for (ST value : in) {
			if (result == null) result = val(value); else result+=" "+val(value);
		}
		return result;
	}
	
	public Period transform(IVLTS period) {
		if (period == null) return null;
		Period result = new Period();
		if (period.getLow() != null) result.setStartElement(transform(period.getLow()));
		if (period.getHigh() != null) result.setEndElement(transform(period.getHigh()));		
		return result;
	}
	
	public DateTimeType transform(IVXBTS date) {
		DateTimeType result = DateTimeType.parseV3(date.getValue());			
		return result;
	}
	
	public DateType transform(TS date) {
		if (date == null) return null;
		DateType result = DateType.parseV3(date.getValue());		
		return result;
	}
	
	public CodeableConcept transform(CE in) {
		if (in == null) return null;
		CodeableConcept cc = new CodeableConcept();
		cc.addCoding().setSystem(in.getCodeSystem()).setCode(in.getCode()).setDisplay(in.getDisplayName());
		return cc;
	}
	
	public <T extends ENXP> StringType withQualifier(T namePart, StringType fhirNamePart) {
		if (namePart.getQualifier() != null) {
			fhirNamePart.addExtension("http://hl7.org/fhir/StructureDefinition/iso21090-EN-qualifier", new StringType(namePart.getQualifier()));
		}			
		fhirNamePart.setValue(val(namePart));
        return fhirNamePart;
	}
	
	public Address transform(AD address) {
		Address addr = new Address();
	    addr.setCity(val(address.getCity()));
		addr.setCountry(val(address.getCountry()));
		addr.setDistrict(val(address.getCounty()));
		addr.setPostalCode(val(address.getPostalCode()));
		addr.setState(val(address.getState()));
		// TODO Missing: type, use
		for (AdxpStreetAddressLine line : address.getStreetAddressLine()) {
			addr.addLine(val(line));
		}
		if (address.getUseablePeriod() != null) {
			//addr.setPeriod(transform(address.getUseablePeriod().get(0)));
		}
		if (address.getUse() != null) {
			switch(address.getUse()) {
			case "H":addr.setUse(AddressUse.HOME);break;
			case "WP":addr.setUse(AddressUse.WORK);break;
			case "TMP":addr.setUse(AddressUse.TEMP);break;
			case "OLD":addr.setUse(AddressUse.OLD);break;
			}
		}
				
		return addr;
	}
	
	public ContactPoint transform(TEL telecom) {
		ContactPoint contactPoint = new ContactPoint();
    			      
		String use = telecom.getUse();
		if (use != null) {
			switch(use) {
			case "HP":contactPoint.setUse(ContactPoint.ContactPointUse.HOME);break;
			
			}
		}
		contactPoint.setValue(telecom.getValue());
    	    	    	    
    	return contactPoint;
	}
	
	public List<Patient> translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			// FIX for xmlns:xmlns
	    String content = new String(input);
	    content = content.replace("xmlns:xmlns","xmlns:xxxxx");
	    
	    List<Patient> response = new ArrayList<Patient>();
	    
        PRPAIN201306UV02Type msg = HL7V3Transformer.unmarshallMessage(PRPAIN201306UV02Type.class, new ByteArrayInputStream(content.getBytes()));
      
		PRPAIN201306UV02MFMIMT700711UV01ControlActProcess controlAct = msg.getControlActProcess();
				
		// OK NF AE
		String queryResponseCode = controlAct.getQueryAck().getQueryResponseCode().getCode();
		if ("NF".equals(queryResponseCode)) {			
			throw new ResourceNotFoundException("sourceIdentifier Patient Identifier not found", error(IssueType.NOTFOUND, "sourceIdentifier Patient Identifier not found"));
		}
		if ("AE".equals(queryResponseCode)) {
			throw new InvalidRequestException("sourceIdentifier Assigning Authority not found", error(IssueType.INVALID, "sourceIdentifier Assigning Authority not found"));
		}
		
		List<PRPAIN201306UV02MFMIMT700711UV01Subject1> subjects = controlAct.getSubject();
		for (PRPAIN201306UV02MFMIMT700711UV01Subject1 subject : subjects) {
			PRPAIN201306UV02MFMIMT700711UV01RegistrationEvent registrationEvent = subject.getRegistrationEvent();
			PRPAIN201306UV02MFMIMT700711UV01Subject2 subject1 = registrationEvent.getSubject1();
			PRPAMT201310UV02Patient patient = subject1.getPatient();
			PRPAMT201310UV02Person patientPerson = patient.getPatientPerson();
			
			if (patient.getId().isEmpty()) continue;
			
			Patient result = new Patient();
			
			boolean idadded = false;
			
			for (II patientId : patient.getId()) {
				result.addIdentifier().setSystem(patientId.getRoot()).setValue(patientId.getExtension());
				
				if (!idadded) {
					result.setId(patientRefCreator.createPatientId(patientId.getRoot(), patientId.getExtension()));
					idadded = true;
				}
			}
			
			CS statusCode = patient.getStatusCode();
			if (statusCode != null && "active".equals(statusCode.getCode())) result.setActive(true);
			
			for (PN name : patientPerson.getName()) {
				HumanName humanName = new HumanName();
				for (EnFamily family : name.getFamily()) {
					if ("BR".equals(family.getQualifier())) {
						humanName.setUse(NameUse.MAIDEN);
					}
					humanName.setFamily(val(family));	
				}
				for (EnGiven given : name.getGiven()) {
					withQualifier(given, humanName.addGivenElement());
				}
				for (EnPrefix prefix : name.getPrefix()) {
					withQualifier(prefix, humanName.addPrefixElement());
				}
				for (EnSuffix suffix : name.getSuffix()) {
					withQualifier(suffix, humanName.addSuffixElement());					
				}
				if (name.getValidTime() != null) humanName.setPeriod(transform(name.getValidTime()));
				result.addName(humanName);				
			}
			
			CE gender = patientPerson.getAdministrativeGenderCode();
			if (gender != null) {
				switch (gender.getCode()) {
					case "M":result.setGender(AdministrativeGender.MALE);break;
					case "F":result.setGender(AdministrativeGender.FEMALE);break;
					case "A":result.setGender(AdministrativeGender.OTHER);break;
					case "U":result.setGender(AdministrativeGender.UNKNOWN);break;				
				}
			}
			TS birthTime = patientPerson.getBirthTime();
			if (birthTime != null) {
				result.setBirthDateElement(transform(birthTime));
			}
			for (AD ad : patientPerson.getAddr()) {
			  result.addAddress(transform(ad));
			}
			for (TEL tel : patientPerson.getTelecom()) {
				result.addTelecom(transform(tel));
			}
			for (PRPAMT201310UV02LanguageCommunication lang : patientPerson.getLanguageCommunication()) {
				CE langCode = lang.getLanguageCode();
				PatientCommunicationComponent pcc = new PatientCommunicationComponent();				
				pcc.setLanguage(transform(langCode));
				BL preferred = lang.getPreferenceInd();
				if (preferred != null && preferred.getValue().booleanValue()) pcc.setPreferred(true);
				result.addCommunication(pcc);
			}
			
			TS deceasedTime = patientPerson.getDeceasedTime();
			if (deceasedTime != null) result.setDeceased(transform(deceasedTime));
			else {
				BL deceased = patientPerson.getDeceasedInd();
				if (deceased != null) result.setDeceased(new BooleanType(deceased.getValue().booleanValue()));
			}
			
			INT multiBirthOrder = patientPerson.getMultipleBirthOrderNumber();
			if (multiBirthOrder != null) {
				result.setMultipleBirth(new IntegerType(multiBirthOrder.getValue()));
			} else {			
				BL multipleBirth = patientPerson.getMultipleBirthInd();
				if (multipleBirth != null) result.setMultipleBirth(new BooleanType(multipleBirth.getValue().booleanValue()));
			}
			
		    CE maritalStatus = patientPerson.getMaritalStatusCode();
		    result.setMaritalStatus(transform(maritalStatus));		    
			
		    for (PRPAMT201310UV02PersonalRelationship relationShip : patientPerson.getPersonalRelationship()) {
		    	CE code = relationShip.getCode();
		    	if (code != null && "MTH".equals(code.getCode()) && "2.16.840.1.113883.12.63".equals(code.getCodeSystem())) {
		    		COCTMT030007UVPerson holder = relationShip.getRelationshipHolder1();
		    		if (holder != null && !holder.getName().isEmpty()) {
		    			EN name = holder.getName().get(0);
		    			if (!name.getFamily().isEmpty()) {
		    				String familyName = val(name.getFamily());
		    				result.addExtension("http://hl7.org/fhir/StructureDefinition/patient-mothersMaidenName", new StringType(familyName));
		    			}
		    		}
		    	}
		    }
		    
			response.add(result);
		}
		
						
		return response;
		} catch (JAXBException e) {
			e.printStackTrace();
			throw new InvalidRequestException("failed parsing response");
		}
			
	}
}
