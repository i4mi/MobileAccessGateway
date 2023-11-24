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
package ch.bfh.ti.i4mi.mag.mhd.iti66;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListMode;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.codesystems.ListStatus;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Recipient;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Telecom;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.BaseQueryResponseConverter;

/**
 * ITI-66 from ITI-18 response converter
 * @author alexander kreutz
 *
 */
public class Iti66ResponseConverter extends BaseQueryResponseConverter {
    
	public Iti66ResponseConverter(final Config config) {
	   super(config);
	}
	
	
	/**
	 * convert ITI-18 query response to ITI-66 response bundle
	 */
    @Override
    public List<ListResource> translateToFhir(QueryResponse input, Map<String, Object> parameters) {
        ArrayList<ListResource> list = new ArrayList<ListResource>();
        if (input != null && Status.SUCCESS.equals(input.getStatus())) {
        	Map<String, ListResource> targetList = new HashMap<String, ListResource>(); 
            if (input.getSubmissionSets() != null) {            	
                for (SubmissionSet submissionSet : input.getSubmissionSets()) {
                	ListResource documentManifest = new ListResource();
                    
                    documentManifest.setId(noUuidPrefix(submissionSet.getEntryUuid()));  
                    
                    documentManifest.setCode(new CodeableConcept(new Coding("https://profiles.ihe.net/ITI/MHD/CodeSystem/MHDlistTypes","submissionset","Submission Set")));
                    targetList.put(documentManifest.getId(), documentManifest);
                    
                    list.add(documentManifest);
                    // limitedMetadata -> meta.profile canonical [0..*]       
                    if (submissionSet.isLimitedMetadata()) {
                    	documentManifest.getMeta().addProfile("https://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.SubmissionSet");
                    } else {
                    	documentManifest.getMeta().addProfile("https://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.SubmissionSet");
                    }
                    
                    // comment -> text Narrative [0..1]
                    LocalizedString comments = submissionSet.getComments();
                    if (comments!=null) {
                    	documentManifest.addNote().setText(comments.getValue());                    	
                    }
                    
                    // uniqueId -> masterIdentifier Identifier [0..1] [1..1]
                    if (submissionSet.getUniqueId()!=null) {
                        documentManifest.addIdentifier((new Identifier().setUse(IdentifierUse.USUAL).setSystem("urn:ietf:rfc:3986").setValue("urn:oid:"+submissionSet.getUniqueId())));
                    }
                    
                    // entryUUID -> identifier Identifier [0..*]
                    if (submissionSet.getEntryUuid()!=null) {
                        documentManifest.addIdentifier((new Identifier().setUse(IdentifierUse.OFFICIAL).setSystem("urn:ietf:rfc:3986").setValue(asUuid(submissionSet.getEntryUuid()))));
                    }
                    // availabilityStatus -> status code {DocumentReferenceStatus} [1..1]
                    //   approved -> status=current Other status values are allowed but are not defined in this mapping to XDS.
                    if (AvailabilityStatus.APPROVED.equals(submissionSet.getAvailabilityStatus())) {
                        documentManifest.setStatus(ListResource.ListStatus.CURRENT);
                    }
                    
                    documentManifest.setMode(ListMode.WORKING);
                    
                    // contentTypeCode -> type CodeableConcept [0..1]
                    if (submissionSet.getContentTypeCode()!=null) {
                        documentManifest
                          .addExtension()
                          .setUrl("https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType")
                          .setValue(transform(submissionSet.getContentTypeCode()));
                    }
                    
                    // patientId -> subject Reference(Patient| Practitioner| Group| Device) [0..1], Reference(Patient)
                    if (submissionSet.getPatientId()!=null) {
                    	Identifiable patient = submissionSet.getPatientId();                    	
                    	documentManifest.setSubject(transformPatient(patient));
                    }
                    
                    // submissionTime -> created dateTime [0..1]
                    if (submissionSet.getSubmissionTime()!=null) {
                        documentManifest.setDate(Date.from(submissionSet.getSubmissionTime().getDateTime().toInstant()));
                    }

                    // authorInstitution, authorPerson, authorRole, authorSpeciality, authorTelecommunication -> author Reference(Practitioner| PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]
                    if (submissionSet.getAuthors() != null) {
                    	for (Author author : submissionSet.getAuthors()) {
                    		documentManifest.setSource(transformAuthor(author));
                    	}
                    }
                    
                    // intendedRecipient -> recipient Reference(Patient| Practitioner| PractitionerRole| RelatedPerson| Organization) [0..*]
                    List<Recipient> recipients = submissionSet.getIntendedRecipients();
                    for (Recipient recipient : recipients) { 
                    	Organization org = recipient.getOrganization();
                    	Person person = recipient.getPerson();                    	
                    	ContactPoint contact = transform(recipient.getTelecom());
                    	var organization = transform(org);
                    	Practitioner practitioner = transformPractitioner(person);
                    	if (organization != null && practitioner == null) {
                    		if (contact != null) organization.addTelecom(contact);
                    		documentManifest
                    		  .addExtension()
                    		  .setUrl("https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-intendedRecipient")
                    		  .setValue(new Reference().setResource(organization));
                    	} else if (organization != null && practitioner != null) {
                    		PractitionerRole role = new PractitionerRole();
                    		role.setPractitioner((Reference) new Reference().setResource(practitioner));
                    		role.setOrganization((Reference) new Reference().setResource(organization));
                    		if (contact != null) role.addTelecom(contact);
                    		documentManifest
	                    		.addExtension()
	                  		    .setUrl("https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-intendedRecipient")
	                  		    .setValue(new Reference().setResource(role));                    		                    		
                    	} else if (organization == null && practitioner != null) {                    		
                    		// May be a patient, related person or practitioner
                    	}                    	
                    }
                    
                    // sourceId -> source uri [0..1] [1..1]
                    if (submissionSet.getSourceId()!=null) {
                        documentManifest
                          .addExtension()
                          .setUrl("https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId")
                          .setValue(new Identifier().setValue("urn:oid:"+submissionSet.getSourceId()));
                    }
                    // title -> description string [0..1]
                    LocalizedString title = submissionSet.getTitle();
                    if (title != null) {
                      documentManifest.setTitle(title.getValue());
                    }
                                                                                                                                         
                }
            }
            if (input.getAssociations() != null) {
            	for (Association ass : input.getAssociations()) {
            		AssociationType tt = ass.getAssociationType();
            		String source = ass.getSourceUuid();
            		String target = ass.getTargetUuid();
            		if (tt == AssociationType.HAS_MEMBER) {
            			ListResource s = targetList.get(noUuidPrefix(source));            			
            			if (s!=null) {            			
            				s.addEntry().setItem(new Reference().setReference("DocumentReference/"+noUuidPrefix(target)));
            			}
            		}            		
            	}
            }
        } else {
        	processError(input);
        }
        return list;
    }
    
    

}
