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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.LinkType;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Patient.PatientLinkComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.pmir.PMIRRequestConverter;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCity;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCountry;
import net.ihe.gazelle.hl7v3.datatypes.AdxpPostalCode;
import net.ihe.gazelle.hl7v3.datatypes.AdxpState;
import net.ihe.gazelle.hl7v3.datatypes.AdxpStreetAddressLine;
import net.ihe.gazelle.hl7v3.datatypes.BL;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.EnPrefix;
import net.ihe.gazelle.hl7v3.datatypes.EnSuffix;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.INT;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.mfmimt700701UV01.MFMIMT700701UV01PriorRegisteredRole;
import net.ihe.gazelle.hl7v3.mfmimt700701UV01.MFMIMT700701UV01PriorRegistration;
import net.ihe.gazelle.hl7v3.mfmimt700701UV01.MFMIMT700701UV01ReplacementOf;
import net.ihe.gazelle.hl7v3.mfmimt700701UV01.MFMIMT700701UV01Subject3;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02Type;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02Type;
import net.ihe.gazelle.hl7v3.prpain201304UV02.PRPAIN201304UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201304UV02.PRPAIN201304UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201304UV02.PRPAIN201304UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201304UV02.PRPAIN201304UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201304UV02.PRPAIN201304UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02LanguageCommunication;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02PatientId;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02PatientPatientPerson;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02PatientStatusCode;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201303UV02.PRPAMT201303UV02LanguageCommunication;
import net.ihe.gazelle.hl7v3.prpamt201303UV02.PRPAMT201303UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201303UV02.PRPAMT201303UV02Person;
import net.ihe.gazelle.hl7v3.voc.ActClass;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.ActMood;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.ParticipationTargetSubject;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * ITI-93 Patient Feed : Merge Request
 * @author alexander
 *
 */
public class Iti93MergeRequestConverter extends Iti93UpdateRequestConverter {

	
	/**
	 * merge request for ITI-93 transaction
	 * @param header
	 * @param entriesByReference
	 * @return
	 * @throws JAXBException
	 */
	public String doMerge(MessageHeader header, Map<String, BundleEntryComponent> entriesByReference) throws JAXBException {
				
		 PRPAIN201304UV02Type resultMsg = new PRPAIN201304UV02Type();
		  resultMsg.setITSVersion("XML_1.0");
		  //String UUID.randomUUID().toString();
		  resultMsg.setId(new II("1.3.6.1.4.1.12559.11.1.2.2.5.7.1", "351"));
		  resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
		  resultMsg.setProcessingCode(new CS("T", null ,null));
		  resultMsg.setProcessingModeCode(new CS("T", null, null));
		  resultMsg.setInteractionId(new II("2.16.840.1.113883.1.18", "PRPA_IN201304UV02Type"));
		  resultMsg.setAcceptAckCode(new CS("AL", null, null));
		
		  MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		  resultMsg.addReceiver(receiver);
		  receiver.setTypeCode(CommunicationFunctionType.RCV);
		  
		  MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		  receiver.setDevice(receiverDevice );
		  receiverDevice.setClassCode(EntityClassDevice.DEV);
		  receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  receiverDevice.setId(Collections.singletonList(new II("1.3.6.1.4.1.12559.11.1.2.2.5.11", null)));
		  
		  MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		  resultMsg.setSender(sender);
		  sender.setTypeCode(CommunicationFunctionType.SND);
		  
		  MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		  sender.setDevice(senderDevice);
		  senderDevice.setClassCode(EntityClassDevice.DEV);
		  senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  senderDevice.setId(Collections.singletonList(new II("1.3.6.1.4.1.12559.11.1.2.2.5.7", null)));
		 		  
		  PRPAIN201304UV02MFMIMT700701UV01ControlActProcess controlActProcess = new PRPAIN201304UV02MFMIMT700701UV01ControlActProcess();
		  resultMsg.setControlActProcess(controlActProcess);
		  controlActProcess.setClassCode(ActClassControlAct.CACT); // ???
		  controlActProcess.setMoodCode(XActMoodIntentEvent.EVN); // ???
		  controlActProcess.setCode(new CD("PRPA_TE201304UV02","2.16.840.1.113883.1.18", null)); // ???
		
		  
		  
		  
		
		  for (Reference ref : header.getFocus()) {
			  BundleEntryComponent entry = entriesByReference.get(ref.getReference());

	    		HTTPVerb method = entry.getRequest().getMethod();
		    	if (method == null) throw new InvalidRequestException("HTTP verb missing in Bundle for Patient resource.");
		    			    			    	
		    	Patient basePatient = (Patient) entry.getResource();
		    	Patient in = null;
		    	List<Patient> replaced = new ArrayList<Patient>();
		    	
		    	for (PatientLinkComponent linked : basePatient.getLink()) {		    		
		    		if (linked.getType().equals(LinkType.REPLACEDBY)) {
		    			Reference other = linked.getOther();	
		    			Patient otherPatient = findPatient(other, entriesByReference, basePatient);
		    			in = otherPatient;
		    			replaced.add(basePatient);
		    		} else if (linked.getType().equals(LinkType.REPLACES)) {
		    			Reference other = linked.getOther();	
		    			Patient otherPatient = findPatient(other, entriesByReference, basePatient);
		    			in = basePatient;
		    			replaced.add(otherPatient);
		    		}
		    	}
		    	
		    	if (in==null || replaced.isEmpty()) throw new InvalidRequestException("Cannot determine Patients to merge");
		    			    		    			    	
		    	PRPAIN201304UV02MFMIMT700701UV01Subject1 subject = new PRPAIN201304UV02MFMIMT700701UV01Subject1();
			  controlActProcess.addSubject(subject);
			  subject.setTypeCode("SUBJ");
			  subject.setContextConductionInd(false); // ???
			  			  
			  PRPAIN201304UV02MFMIMT700701UV01RegistrationEvent registrationEvent = new PRPAIN201304UV02MFMIMT700701UV01RegistrationEvent();
			  subject.setRegistrationEvent(registrationEvent);
			  registrationEvent.setClassCode(ActClass.REG);
			  registrationEvent.setMoodCode(ActMood.EVN);
			  registrationEvent.setStatusCode(new CS("active",null,null)); // ???
			  
			  List<MFMIMT700701UV01ReplacementOf> replacementOfList = new ArrayList<MFMIMT700701UV01ReplacementOf>();
			  registrationEvent.setReplacementOf(replacementOfList);
			  
			  for (Patient replacedPatient : replaced) {
			    MFMIMT700701UV01ReplacementOf replacementOf = new MFMIMT700701UV01ReplacementOf();
			    MFMIMT700701UV01PriorRegistration priorRegistration = new MFMIMT700701UV01PriorRegistration();
			    replacementOf.setPriorRegistration(priorRegistration );
			    replacementOf.setTypeCode("RPLC");
			    priorRegistration.setClassCode(ActClass.REG);
			    priorRegistration.setMoodCode(ActMood.EVN);
			    
			    MFMIMT700701UV01Subject3 subject1 = new MFMIMT700701UV01Subject3();
				priorRegistration.setSubject1(subject1 );
			    subject1.setTypeCode(ParticipationTargetSubject.SBJ);

			    MFMIMT700701UV01PriorRegisteredRole priorRegisteredRole = new MFMIMT700701UV01PriorRegisteredRole();
				subject1.setPriorRegisteredRole(priorRegisteredRole );
				priorRegisteredRole.setClassCode("PAT");
				
				for (Identifier id : replacedPatient.getIdentifier()) {
					priorRegisteredRole.addId(patientIdentifier(id));
			    }								
			    
			    replacementOfList.add(replacementOf);
			  }
			  
			  			  
			  PRPAIN201304UV02MFMIMT700701UV01Subject2 subject1 = new PRPAIN201304UV02MFMIMT700701UV01Subject2();			  
			  registrationEvent.setSubject1(subject1);
			  subject1.setTypeCode(ParticipationTargetSubject.SBJ);
			  			  
			  PRPAMT201303UV02Patient patient = new PRPAMT201303UV02Patient();
			  subject1.setPatient(patient);
			  			  
			  PRPAMT201302UV02PatientStatusCode statusCode = new PRPAMT201302UV02PatientStatusCode("active", null, null);
			  patient.setStatusCode(statusCode); //???
			  			  
			  PRPAMT201303UV02Person patientPerson = new PRPAMT201303UV02Person();
			  patient.setPatientPerson(patientPerson );
		    	
			  // TODO How is the correct mapping done?
			    for (Identifier id : in.getIdentifier()) {
			    	PRPAMT201302UV02PatientId patientId = new PRPAMT201302UV02PatientId(getScheme(id.getSystem()),id.getValue());
					patient.addId(patientId );
			    }
		    	
		    	for (HumanName name : in.getName()) {
		    		PN nameElement = new PN();
		    		if (name.hasFamily()) nameElement.addFamily(element(EnFamily.class, name.getFamily()));
		    		for (StringType given : name.getGiven()) nameElement.addGiven(element(EnGiven.class, given.getValue()));
		    		for (StringType prefix : name.getPrefix()) nameElement.addPrefix(element(EnPrefix.class, prefix.getValue()));
		    		for (StringType suffix : name.getSuffix()) nameElement.addSuffix(element(EnSuffix.class, suffix.getValue()));
		    		if (name.hasPeriod()) nameElement.addValidTime(transform(name.getPeriod()));		    		
					patientPerson.addName(nameElement );	
		    	}
		    	
		    	
		    	patientPerson.setBirthTime(transform(in.getBirthDateElement()));
		    	if (in.hasGender()) {
			        switch(in.getGender()) {
			        case MALE:patientPerson.setAdministrativeGenderCode(new CE("M","Male","2.16.840.1.113883.12.1"));break;
			        case FEMALE:patientPerson.setAdministrativeGenderCode(new CE("F","Female","2.16.840.1.113883.12.1"));break;
			        case OTHER:patientPerson.setAdministrativeGenderCode(new CE("A","Ambiguous","2.16.840.1.113883.12.1"));break;
			        case UNKNOWN:patientPerson.setAdministrativeGenderCode(new CE("U","Unknown","2.16.840.1.113883.12.1"));break;
			        }
		    	}
		        
		        for (Address address : in.getAddress()) {
		        	AD addr = new AD();

		        	// TODO Missing: district, type, use
		        	if (address.hasCity()) addr.addCity(element(AdxpCity.class, address.getCity()));
		        	if (address.hasCountry()) addr.addCountry(element(AdxpCountry.class, address.getCountry()));
		        	if (address.hasPostalCode()) addr.addPostalCode(element(AdxpPostalCode.class, address.getPostalCode()));
		        	if (address.hasState()) addr.addState(element(AdxpState.class, address.getState()));
		        	if (address.hasLine()) for (StringType line : address.getLine()) addr.addStreetAddressLine(element(AdxpStreetAddressLine.class, line.getValue()));
		        	if (address.hasPeriod()) addr.addUseablePeriod(transform(address.getPeriod()));
					patientPerson.addAddr(addr);
		        }
		    	
		        for (ContactPoint contactPoint : in.getTelecom()) {
                    TEL telecom = new TEL();
		        			        		        	
		        	ContactPoint.ContactPointUse use = contactPoint.getUse();		        	
		        	if (use != null) telecom.setUse(use.toString());
		        	telecom.setValue(contactPoint.getValue());
		        	if (contactPoint.hasPeriod()) telecom.addUseablePeriod(transform(contactPoint.getPeriod()));
		        	
					patientPerson.addTelecom(telecom);
		        }
		        
		        if (in.hasDeceasedBooleanType()) {
		          patientPerson.setDeceasedInd(new BL(in.getDeceasedBooleanType().getValue()));
		        }
		        if (in.hasDeceasedDateTimeType()) {
		        	patientPerson.setDeceasedTime(transform(in.getDeceasedDateTimeType()));
		        }
		        if (in.hasMultipleBirthBooleanType()) {
		        	patientPerson.setMultipleBirthInd(new BL(in.getMultipleBirthBooleanType().getValue()));
		        }
		        if (in.hasMultipleBirthIntegerType()) {
		        	patientPerson.setMultipleBirthOrderNumber(new INT(in.getMultipleBirthIntegerType().getValue()));
		        }
		        if (in.hasMaritalStatus()) {
		        	patientPerson.setMaritalStatusCode(transform(in.getMaritalStatus()));
		        }
		        if (in.hasCommunication()) {
		        	for (PatientCommunicationComponent pcc : in.getCommunication()) {		        		
		        		PRPAMT201303UV02LanguageCommunication languageCommunication = new PRPAMT201303UV02LanguageCommunication();
		        		languageCommunication.setLanguageCode(transform(pcc.getLanguage()));
		        		if (pcc.hasPreferred()) languageCommunication.setPreferenceInd(new BL(pcc.getPreferred()));
						patientPerson.addLanguageCommunication(languageCommunication);
		        	}
		        }
	        		        		    	
	    	}
	   
	    
	    ByteArrayOutputStream out = new ByteArrayOutputStream();	    
	    HL7V3Transformer.marshallMessage(PRPAIN201304UV02Type.class, out, resultMsg);
	    System.out.println("POST CONVERT");
	    String outArray = new String(out.toByteArray()); 
	    System.out.println(outArray);
	    return outArray;
	}
}