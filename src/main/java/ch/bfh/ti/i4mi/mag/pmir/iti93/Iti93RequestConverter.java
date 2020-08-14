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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Organization.OrganizationContactComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.PatientCommunicationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.ContactPointUse;
import org.hl7.fhir.r4.model.codesystems.LinkType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.PMIRRequestConverter;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01AssignedEntity;
import net.ihe.gazelle.hl7v3.coctmt090003UV01.COCTMT090003UV01Organization;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03ContactParty;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03Organization;
import net.ihe.gazelle.hl7v3.coctmt150003UV03.COCTMT150003UV03Person;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCity;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCountry;
import net.ihe.gazelle.hl7v3.datatypes.AdxpPostalCode;
import net.ihe.gazelle.hl7v3.datatypes.AdxpState;
import net.ihe.gazelle.hl7v3.datatypes.AdxpStreetAddressLine;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.EnPrefix;
import net.ihe.gazelle.hl7v3.datatypes.EnSuffix;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.ON;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
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
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01RegistrationEvent;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01Subject1;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02MFMIMT700701UV01Subject2;
import net.ihe.gazelle.hl7v3.prpain201302UV02.PRPAIN201302UV02Type;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02QUQIMT021001UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Patient;
import net.ihe.gazelle.hl7v3.prpamt201301UV02.PRPAMT201301UV02Person;
import net.ihe.gazelle.hl7v3.prpamt201302UV02.PRPAMT201302UV02Patient;
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
 * ITI-93 to ITI-44 request converter
 * @author alexander kreutz
 *
 */
public class Iti93RequestConverter extends Iti93MergeRequestConverter {

    /**
     * convert ITI-93 to ITI-44 request
     * @param requestBundle
     * @return
     * @throws JAXBException
     */
	public String iti93ToIti44Converter(@Body Bundle requestBundle) throws JAXBException {
		if (requestBundle.getType() != BundleType.MESSAGE) throw new InvalidRequestException("Bundle type must be message");
		
		BundleEntryComponent headerComponent = requestBundle.getEntryFirstRep();
		if (headerComponent == null) throw new InvalidRequestException("First bundle entry must be MessageHeader.");
		Resource headerResource = headerComponent.getResource();
		if (headerResource==null || !(headerResource instanceof MessageHeader)) throw new InvalidRequestException("First bundle entry must be MessageHeader.");
		MessageHeader header = (MessageHeader) headerResource;
		
		if (!("urn:ihe:iti:pmir:2019:patient-feed".equals(header.getEventUriType().getValue()))) throw new InvalidRequestException("Wrong eventUri");
		
		Map<String, BundleEntryComponent> entriesByReference = new HashMap<String, BundleEntryComponent>();
		for (BundleEntryComponent entry : requestBundle.getEntry()) {	    	
	    	if (entry.getResource() instanceof Patient) {
	    		System.out.println("REG:"+entry.getResource().getIdElement().getIdPart());
	    		entriesByReference.put("Patient/"+entry.getResource().getIdElement().getIdPart(), entry);
	    	}
		}
			
		Reference ref = header.getFocusFirstRep();
		BundleEntryComponent entry = entriesByReference.get(ref.getReference());
		System.out.println(ref.getReference()+" found="+entry);
		HTTPVerb method = entry.getRequest().getMethod();
		if (method == null) throw new InvalidRequestException("HTTP verb missing in Bundle for Patient resource.");
		
		if (method.equals(HTTPVerb.POST)) {
			return doCreate(header, entriesByReference);
		} else if (method.equals(HTTPVerb.PUT)) {
			if (((Patient) entry.getResource()).getActive()==false) {
				return doMerge(header, entriesByReference);
			} else {
				return doUpdate(header, entriesByReference);
			}
		} else if (method.equals(HTTPVerb.DELETE)) {
			return doUpdate(header, entriesByReference);
		}
		
		throw new InvalidRequestException("Cannot handle request");
	}
	
	
}
