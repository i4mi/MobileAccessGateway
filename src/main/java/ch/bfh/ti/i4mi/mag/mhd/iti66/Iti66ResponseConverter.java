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
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
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
    public List<DocumentManifest> translateToFhir(QueryResponse input, Map<String, Object> parameters) {
        ArrayList<DocumentManifest> list = new ArrayList<DocumentManifest>();
        if (input != null && Status.SUCCESS.equals(input.getStatus())) {
        	
            if (input.getSubmissionSets() != null) {            	
                for (SubmissionSet submissionSet : input.getSubmissionSets()) {
                    DocumentManifest documentManifest = new DocumentManifest();
                    
                    documentManifest.setId(submissionSet.getEntryUuid());  
                    
                    list.add(documentManifest);
                    // limitedMetadata -> meta.profile canonical [0..*]       
                    if (submissionSet.isLimitedMetadata()) {
                    	documentManifest.getMeta().addProfile("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Minimal_DocumentManifest");
                    } else {
                    	documentManifest.getMeta().addProfile("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Comprehensive_DocumentManifest");
                    }
                    
                    // comment -> text Narrative [0..1]
                    LocalizedString comments = submissionSet.getComments();
                    if (comments!=null) {
                    	documentManifest.setText(transformToNarrative(comments));                    	
                    }
                    
                    // uniqueId -> masterIdentifier Identifier [0..1] [1..1]
                    if (submissionSet.getUniqueId()!=null) {
                        documentManifest.setMasterIdentifier((new Identifier().setValue("urn:oid:"+submissionSet.getUniqueId())));
                    }
                    
                    // entryUUID -> identifier Identifier [0..*]
                    if (submissionSet.getEntryUuid()!=null) {
                        documentManifest.addIdentifier((new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:"+submissionSet.getEntryUuid())));
                    }
                    // availabilityStatus -> status code {DocumentReferenceStatus} [1..1]
                    //   approved -> status=current Other status values are allowed but are not defined in this mapping to XDS.
                    if (AvailabilityStatus.APPROVED.equals(submissionSet.getAvailabilityStatus())) {
                        documentManifest.setStatus(DocumentReferenceStatus.CURRENT);
                    }
                    
                    // contentTypeCode -> type CodeableConcept [0..1]
                    if (submissionSet.getContentTypeCode()!=null) {
                        documentManifest.setType(transform(submissionSet.getContentTypeCode()));
                    }
                    
                    // patientId -> subject Reference(Patient| Practitioner| Group| Device) [0..1], Reference(Patient)
                    if (submissionSet.getPatientId()!=null) {
                    	Identifiable patient = submissionSet.getPatientId();                    	
                    	documentManifest.setSubject(transformPatient(patient));
                    }
                    
                    // submissionTime -> created dateTime [0..1]
                    if (submissionSet.getSubmissionTime()!=null) {
                        documentManifest.setCreated(Date.from(submissionSet.getSubmissionTime().getDateTime().toInstant()));
                    }

                    // authorInstitution, authorPerson, authorRole, authorSpeciality, authorTelecommunication -> author Reference(Practitioner| PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]
                    if (submissionSet.getAuthors() != null) {
                    	for (Author author : submissionSet.getAuthors()) {
                    		documentManifest.addAuthor(transformAuthor(author));
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
                    		documentManifest.addRecipient().setResource(organization);
                    	} else if (organization != null && practitioner != null) {
                    		PractitionerRole role = new PractitionerRole();
                    		role.setPractitioner((Reference) new Reference().setResource(practitioner));
                    		role.setOrganization((Reference) new Reference().setResource(organization));
                    		if (contact != null) role.addTelecom(contact);
                    		documentManifest.addRecipient().setResource(role);
                    	} else if (organization == null && practitioner != null) {                    		
                    		// May be a patient, related person or practitioner
                    	}                    	
                    }
                    
                    // sourceId -> source uri [0..1] [1..1]
                    if (submissionSet.getSourceId()!=null) {
                        documentManifest.setSource("urn:oid:"+submissionSet.getSourceId());
                    }
                    // title -> description string [0..1]
                    LocalizedString title = submissionSet.getTitle();
                    if (title != null) {
                      documentManifest.setDescription(title.getValue());
                    }
                    
                    // References to DocumentReference Resources representing DocumentEntry objects in the SubmissionSet or List Resources representing Folder objects in the SubmissionSet. -> content Reference(Any) [1..*] Reference( DocumentReference| List)                                       
                    // homeCommunityId -> Note 2: Not Applicable - The Document Sharing metadata element has no equivalent element in the HL7 FHIR; therefore, a Document Source is not able to set these elements, and Document Consumers will not have access to these elements.
                }
            }
        } else {
        	processError(input);
        }
        return list;
    }
    
    

}
