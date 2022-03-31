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

package ch.bfh.ti.i4mi.mag.pmir.iti104;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.PMIRRequestConverter;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01AssignedEntity;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01Organization;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03ContactParty;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03Organization;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03Person;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.BL;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.INT;
import net.ihe.gazelle.hl7v3.datatypes.ON;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.mfmimt700701UV01.MFMIMT700701UV01Custodian;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201301UV02.PRPAIN201301UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02LanguageCommunication;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02PatientId;
import net.ihe.gazelle.hl7v3.voc.ActClass;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.ActMood;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClass;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityClassOrganization;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.ParticipationTargetSubject;
import net.ihe.gazelle.hl7v3.voc.ParticipationType;
import net.ihe.gazelle.hl7v3.voc.RoleClassAssignedEntity;
import net.ihe.gazelle.hl7v3.voc.RoleClassContact;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * ITI-104 Patient Identity Feed (add a new patient)
 * 
 * @author alexander kreutz
 *
 */
public class Iti104AddRequestConverter extends PMIRRequestConverter {

	@Autowired
	protected Config config;

	/**
	 * add a new patient
	 * @param header
	 * @param entriesByReference
	 * @return
	 * @throws JAXBException
	 */
	public String doCreate(Patient in, Identifier identifier) throws JAXBException {
		
		PRPAIN201301UV02Type resultMsg = new PRPAIN201301UV02Type();		
		  resultMsg.setITSVersion("XML_1.0");
		  //String UUID.randomUUID().toString();
		  resultMsg.setId(new II(config.getPixQueryOid(), uniqueId()));
		  resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
		  resultMsg.setProcessingCode(new CS("T", null ,null));
		  resultMsg.setProcessingModeCode(new CS("T", null, null));
		  resultMsg.setInteractionId(new II("2.16.840.1.113883.1.18", "PRPA_IN201301UV02"));
		  resultMsg.setAcceptAckCode(new CS("AL", null, null));
		
		  MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		  resultMsg.addReceiver(receiver);
		  receiver.setTypeCode(CommunicationFunctionType.RCV);
		  
		  MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		  receiver.setDevice(receiverDevice );
		  receiverDevice.setClassCode(EntityClassDevice.DEV);
		  receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  receiverDevice.setId(Collections.singletonList(new II(config.getPixReceiverOid(), null)));
		  
		  MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		  resultMsg.setSender(sender);
		  sender.setTypeCode(CommunicationFunctionType.SND);
		  
		  MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		  sender.setDevice(senderDevice);
		  senderDevice.setClassCode(EntityClassDevice.DEV);
		  senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  senderDevice.setId(Collections.singletonList(new II(config.getPixMySenderOid(), null)));
		 
		  PRPAIN201301UV02MFMIMT700701UV01ControlActProcess controlActProcess = new PRPAIN201301UV02MFMIMT700701UV01ControlActProcess();		  
		  resultMsg.setControlActProcess(controlActProcess);
		  controlActProcess.setClassCode(ActClassControlAct.CACT); 
		  controlActProcess.setMoodCode(XActMoodIntentEvent.EVN); 
		  controlActProcess.setCode(new CD("PRPA_TE201301UV02",null,"2.16.840.1.113883.1.18")); 
		
		    			    			    	
		    	PRPAIN201301UV02MFMIMT700701UV01Subject1 subject = new PRPAIN201301UV02MFMIMT700701UV01Subject1();		    	
			  controlActProcess.addSubject(subject);
			  subject.setTypeCode("SUBJ");
			  subject.setContextConductionInd(false); // ???
			  
			  PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent registrationEvent = new PRPAIN201301UV02MFMIMT700701UV01RegistrationEvent();			  
			  subject.setRegistrationEvent(registrationEvent);
			  registrationEvent.setClassCode(ActClass.REG);
			  registrationEvent.setMoodCode(ActMood.EVN);
			  registrationEvent.setStatusCode(new CS("active",null,null)); // ???
			  
			  PRPAIN201301UV02MFMIMT700701UV01Subject2 subject1 = new PRPAIN201301UV02MFMIMT700701UV01Subject2();
			  			  
			  registrationEvent.setSubject1(subject1);
			  subject1.setTypeCode(ParticipationTargetSubject.SBJ);
			  
			  PRPAMT201301UV02Patient patient = new PRPAMT201301UV02Patient();			  
			  subject1.setPatient(patient);
			  patient.setClassCode("PAT");
			  			  
			  patient.setStatusCode(new CS("active", null ,null)); //???
			  
			  PRPAMT201301UV02Person patientPerson = new PRPAMT201301UV02Person();
			  patient.setPatientPerson(patientPerson);
			  patientPerson.setClassCode(EntityClass.PSN);
			  patientPerson.setDeterminerCode(EntityDeterminer.INSTANCE);
			  
			  patient.addId(patientIdentifier(identifier));
		    	
			  // TODO How is the correct mapping done?
			    for (Identifier id : in.getIdentifier()) {
			    	patient.addId(patientIdentifier(id));
			    }
			    
		    	
		    	for (HumanName name : in.getName()) {		    				    	
					patientPerson.addName(transform(name));	
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
		        
		    	if (in.hasAddress()) patientPerson.setAddr(new ArrayList<AD>());
		        for (Address address : in.getAddress()) {
					patientPerson.addAddr(transform(address));
		        }
		    	
		        for (ContactPoint contactPoint : in.getTelecom()) {                    
					patientPerson.addTelecom(transform(contactPoint));
		        }
		        
		        List<II> orgIds = new ArrayList<II>();
		        Organization managingOrg = getManagingOrganization(in);
		        // NULL POINTER CHECK
		        if (managingOrg!=null) {
    		        for (Identifier id : managingOrg.getIdentifier()) {
    		        	orgIds.add(new II(getScheme(id.getSystem()), null));
    		        }
		        } else {
		    		Reference org = in.getManagingOrganization();
		    		if (org != null && org.getIdentifier()!=null) {
    		        	orgIds.add(new II(getScheme(org.getIdentifier().getSystem()), null));
		    		}
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
		        		PRPAMT201301UV02LanguageCommunication languageCommunication = new PRPAMT201301UV02LanguageCommunication();
		        		languageCommunication.setLanguageCode(transform(pcc.getLanguage()));
		        		// NULL POINTER EXCEPTION
		        		if (pcc.hasPreferred()) languageCommunication.setPreferenceInd(new BL(pcc.getPreferred()));
						patientPerson.addLanguageCommunication(languageCommunication);
		        	}
		        }
		        
		        COCTMT150003UV03Organization providerOrganization = new COCTMT150003UV03Organization();
				patient.setProviderOrganization(providerOrganization);
				providerOrganization.setClassCode(EntityClassOrganization.ORG);
				providerOrganization.setDeterminerCode(EntityDeterminer.INSTANCE);
		        				
				providerOrganization.setId(orgIds);
				ON name = null;
				if (managingOrg !=null && managingOrg.hasName()) {	
					name = new ON();
					name.setMixed(Collections.singletonList(managingOrg.getName()));
					providerOrganization.setName(Collections.singletonList(name));
				}
				if (managingOrg != null) {
    				COCTMT150003UV03ContactParty contactParty = new COCTMT150003UV03ContactParty();
    				contactParty.setClassCode(RoleClassContact.CON);
                    for (ContactPoint contactPoint : managingOrg.getTelecom()) {
                    	contactParty.addTelecom(transform(contactPoint));
    				}		
                    if (managingOrg.hasAddress()) {
                    	contactParty.setAddr(new ArrayList<AD>());
                        for (Address address : managingOrg.getAddress()) {
                        	contactParty.addAddr(transform(address));
                        }
                    if (managingOrg.hasContact()) {
                    	OrganizationContactComponent occ = managingOrg.getContactFirstRep();
                    	COCTMT150003UV03Person contactPerson = new COCTMT150003UV03Person();
                    	contactPerson.setClassCode(EntityClass.PSN);
                    	contactPerson.setDeterminerCode(EntityDeterminer.INSTANCE);
                        if (occ.hasName()) contactPerson.setName(Collections.singletonList(transform(occ.getName())));                    
        				contactParty.setContactPerson(contactPerson);	
                    }
                    providerOrganization.setContactParty(Collections.singletonList(contactParty));
                }
				
				MFMIMT700701UV01Custodian custodian = new MFMIMT700701UV01Custodian();
				registrationEvent.setCustodian(custodian );
				custodian.setTypeCode(ParticipationType.CST);
				
				COCTMT090003UV01AssignedEntity assignedEntity = new COCTMT090003UV01AssignedEntity();
				custodian.setAssignedEntity(assignedEntity);
				assignedEntity.setClassCode(RoleClassAssignedEntity.ASSIGNED);
				
				List<II> custIds = new ArrayList<II>();			        			       
			    custIds.add(new II(getScheme(config.getCustodianOid()), null));
				
				assignedEntity.setId(custIds);
				//assignedEntity.setId(orgIds);
				
				COCTMT090003UV01Organization assignedOrganization = new COCTMT090003UV01Organization();
				assignedEntity.setAssignedOrganization(assignedOrganization );
				assignedOrganization.setClassCode(EntityClassOrganization.ORG);
				assignedOrganization.setDeterminerCode(EntityDeterminer.INSTANCE);
				if (managingOrg.hasName()) {	
				  assignedOrganization.setName(Collections.singletonList(name));
				}
			}
	    
	    ByteArrayOutputStream out = new ByteArrayOutputStream();	    
	    HL7V3Transformer.marshallMessage(PRPAIN201301UV02Type.class, out, resultMsg);
	    
	    String outArray = new String(out.toByteArray()); 
	    
	    return outArray;
	}

	Organization getManagingOrganization(Patient in) {
		return getManagingOrganization(in, null);
	}

	Organization getManagingOrganization(Patient in, List<Resource> container) {
		Reference org = in.getManagingOrganization();
		if (org == null)
			return null;
		String targetRef = org.getReference();
		List<Resource> resources = container != null ? container : in.getContained();
		for (Resource resource : resources) {
			if (targetRef.equals(resource.getId()) && resource instanceof Organization) {
				return (Organization) resource;
			}
		}
		return null;
	}

	Patient findPatient(Reference ref, Map<String, BundleEntryComponent> entriesbyReference, Patient current) {
		BundleEntryComponent entry = entriesbyReference.get(ref.getReference());
		if (entry != null)
			return (Patient) entry.getResource();
		for (Resource res : current.getContained()) {
			if (ref.getReference().equals(res.getId()))
				return (Patient) res;
		}
		return null;
	}

	public II patientIdentifier(Identifier id) {
		String assigner = null;
		if (id.hasAssigner())
			assigner = id.getAssigner().getDisplay();
		return new II(getScheme(id.getSystem()), id.getValue(), assigner);
	}

	public PRPAMT201302UV02PatientId patientIdentifierUpd(Identifier id) {
		return new PRPAMT201302UV02PatientId(getScheme(id.getSystem()), id.getValue());
	}
}
