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
package ch.bfh.ti.i4mi.mag.mhd.iti67;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.PatientInfo;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Telecom;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.BaseQueryResponseConverter;

public class Iti67ResponseConverter extends BaseQueryResponseConverter {

    private final Config config;

    public Iti67ResponseConverter(final Config config) {
        this.config = config;
    }

    @Override
    public List<DocumentReference> translateToFhir(QueryResponse input, Map<String, Object> parameters) {
        ArrayList<DocumentReference> list = new ArrayList<DocumentReference>();
        if (input != null && Status.SUCCESS.equals(input.getStatus())) {
            if (input.getDocumentEntries() != null) {
                for (DocumentEntry documentEntry : input.getDocumentEntries()) {
                    DocumentReference documentReference = new DocumentReference();

                    // FIXME String uuid = UUID.randomUUID().toString();
                    documentReference.setId(documentEntry.getEntryUuid()); // FIXME do we need to cache this id in
                                                                           // relation to the DocumentManifest itself
                                                                           // for

                    list.add(documentReference);
                    // limitedMetadata -> meta.profile canonical [0..*] TODO
                    // uniqueId -> masterIdentifier Identifier [0..1] [1..1]
                    if (documentEntry.getUniqueId() != null) {
                        documentReference.setMasterIdentifier(
                                (new Identifier().setValue("urn:oid:" + documentEntry.getUniqueId())));
                    }

                    // entryUUID -> identifier Identifier [0..*]
                    // When the DocumentReference.identifier carries the entryUUID then the
                    // DocumentReference.identifier. use shall be ‘official’
                    if (documentEntry.getEntryUuid() != null) {
                        documentReference.addIdentifier((new Identifier().setSystem("urn:ietf:rfc:3986")
                                .setValue("urn:uuid:" + documentEntry.getEntryUuid())).setUse(IdentifierUse.OFFICIAL));
                    }
                    // availabilityStatus -> status code {DocumentReferenceStatus} [1..1]
                    // approved -> status=current
                    // deprecated -> status=superseded
                    // Other status values are allowed but are not defined in this mapping to XDS.
                    if (AvailabilityStatus.APPROVED.equals(documentEntry.getAvailabilityStatus())) {
                        documentReference.setStatus(DocumentReferenceStatus.CURRENT);
                    }
                    if (AvailabilityStatus.DEPRECATED.equals(documentEntry.getAvailabilityStatus())) {
                        documentReference.setStatus(DocumentReferenceStatus.SUPERSEDED);
                    }

                    // contentTypeCode -> type CodeableConcept [0..1]
                    if (documentEntry.getTypeCode() != null) {
                        documentReference.setType(transform(documentEntry.getTypeCode()));
                    }
                    // classCode -> category CodeableConcept [0..*]
                    if (documentEntry.getClassCode() != null) {
                        documentReference.addCategory((transform(documentEntry.getClassCode())));
                    }

                    // patientId -> subject Reference(Patient| Practitioner| Group| Device) [0..1],
                    // Reference(Patient)
                    // Not a contained resource. URL Points to an existing Patient Resource
                    // representing the XDS Affinity Domain Patient.
                    // TODO: This concept needs to be defined
                    if (documentEntry.getPatientId()!=null) {
                    	Identifiable patient = documentEntry.getPatientId();
                    	Identifier id = new Identifier()
                    			.setSystem(patient.getAssigningAuthority().getUniversalId())
                    			.setValue(patient.getId());
                    	documentReference.setSubject(new Reference().setIdentifier(id));
                    }

                    // creationTime -> date instant [0..1]
                    if (documentEntry.getCreationTime() != null) {
                        documentReference.setDate(Date.from(documentEntry.getCreationTime().getDateTime().toInstant()));
                    }

                    // TODO: authorPerson, authorInstitution, authorPerson, authorRole,
                    // authorSpeciality, authorTelecommunication -> author Reference(Practitioner|
                    // PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]                   
                    if (documentEntry.getAuthors() != null) {
                    	for (Author author : documentEntry.getAuthors()) {
                    		Person person = author.getAuthorPerson();
                    		Practitioner containedPerson = transformPractitioner(person);

                    		// TODO
                    		List<Organization> orgs = author.getAuthorInstitution();
 
                    		// TODO
                    		List<Identifiable> roles = author.getAuthorRole();
                    		
                    		// TODO
                    		List<Identifiable> specialities = author.getAuthorSpecialty();
                    		
                    		List<Telecom> telecoms = author.getAuthorTelecom();
                    		for (Telecom telecom : telecoms) containedPerson.addTelecom(transform(telecom));
                    		
                    		documentReference.addContained(containedPerson);
                    		documentReference.addAuthor().setReference(containedPerson.getId());
                    		
                    	}
                    }
                    // TODO: legalAuthenticator -> authenticator Note 1
                    // Reference(Practitioner|Practition erRole|Organization [0..1]
                    Person person = documentEntry.getLegalAuthenticator();
                    if (person != null) {
                       Practitioner practitioner = transformPractitioner(person);
                       documentReference.addContained(practitioner);
                       documentReference.setAuthenticator(new Reference().setReference(practitioner.getId()));
                    }
                    
                    // TODO: Relationship Association -> relatesTo [0..*]                   
                    // TODO: Relationship type -> relatesTo.code code [1..1]
                    // TODO: relationship reference -> relatesTo.target Reference(DocumentReference)
                    // [1..1]

                    // title -> description string [0..1]
                    if (documentEntry.getTitle() != null) {
                        documentReference.setDescription(documentEntry.getTitle().getValue());
                    }

                    // confidentialityCode -> securityLabel CodeableConcept [0..*] Note: This
                    // is NOT the DocumentReference.meta, as that holds the meta tags for the
                    // DocumentReference itself.
                    if (documentEntry.getConfidentialityCodes() != null) {
                        documentReference.addSecurityLabel(transform(documentEntry.getConfidentialityCodes()));
                    }

                    DocumentReferenceContentComponent content = documentReference.addContent();
                    Attachment attachment = new Attachment();
                    content.setAttachment(attachment);

                    // mimeType -> content.attachment.contentType [1..1] code [0..1]
                    if (documentEntry.getMimeType() != null) {
                        attachment.setContentType(documentEntry.getMimeType());
                    }

                    // languageCode -> content.attachment.language code [0..1]
                    if (documentEntry.getLanguageCode() != null) {
                        attachment.setLanguage(documentEntry.getLanguageCode());
                    }

                    // TODO: retrievable location of the document -> content.attachment.url uri
                    // [0..1] [1..1
                    // has to defined, for the PoC we define
                    // $host:port/camel/$repositoryid/$uniqueid
                    attachment.setUrl(config.getUriMagXdsRetrieve() + "?uniqueId=" + documentEntry.getUniqueId()
                            + "&repositoryUniqueId=" + documentEntry.getRepositoryUniqueId());

                    // size -> content.attachment.size integer [0..1] The size is calculated
                    if (documentEntry.getSize() != null) {
                        attachment.setSize(documentEntry.getSize().intValue());
                    }

                    // on the data prior to base64 encoding, if the data is base64 encoded.
                    // TODO: hash -> content.attachment.hash string [0..1]
                    if (documentEntry.getHash()!=null) {
                    	attachment.setHash(documentEntry.getHash().getBytes());
                    }

                    // comments -> content.attachment.title string [0..1]
                    if (documentEntry.getComments() != null) {
                        attachment.setTitle(documentEntry.getComments().getValue());
                    }

                    // TcreationTime -> content.attachment.creation dateTime [0..1]
                    if (documentEntry.getCreationTime() != null) {
                        attachment.setCreation(Date.from(documentEntry.getCreationTime().getDateTime().toInstant()));
                    }

                    // formatCode -> content.format Coding [0..1]
                    if (documentEntry.getFormatCode() != null) {
                        content.setFormat(transform(documentEntry.getFormatCode()).getCodingFirstRep());
                    }

                    DocumentReferenceContextComponent context = new DocumentReferenceContextComponent();
                    documentReference.setContext(context);

                    // TODO: referenceIdList -> context.encounter Reference(Encounter) [0..*] When
                    // referenceIdList contains an encounter, and a FHIR Encounter is available, it
                    // may be referenced.
                    List<ReferenceId> refIds = documentEntry.getReferenceIdList();
                    if (refIds!=null) {
                    	for (ReferenceId refId : refIds) {
                    		if (ReferenceId.ID_TYPE_ENCOUNTER_ID.equals(refId.getIdTypeCode())) {
                    			context.addEncounter(transform(refId));
                    		}
                    	}
                    }
                    
                    // eventCodeList -> context.event CodeableConcept [0..*]
                    if (documentEntry.getEventCodeList()!=null) {
                    	documentReference.getContext().setEvent(transformMultiple(documentEntry.getEventCodeList()));
                    }
                    
                    // serviceStartTime serviceStopTime -> context.period Period [0..1]
                    if (documentEntry.getServiceStartTime()!=null || documentEntry.getServiceStopTime()!=null) {
                    	Period period = new Period();
                    	period.setStartElement(transform(documentEntry.getServiceStartTime()));
                    	period.setEndElement(transform(documentEntry.getServiceStopTime()));
                    	documentReference.getContext().setPeriod(period);
                    }

                    // healthcareFacilityTypeCode -> context.facilityType CodeableConcept
                    // [0..1]
                    if (documentEntry.getHealthcareFacilityTypeCode() != null) {
                        context.setFacilityType(transform(documentEntry.getHealthcareFacilityTypeCode()));
                    }

                    // practiceSettingCode -> context.practiceSetting CodeableConcept [0..1]
                    if (documentEntry.getPracticeSettingCode() != null) {
                        context.setPracticeSetting(transform(documentEntry.getPracticeSettingCode()));
                    }

                    // TODO: sourcePatientId and sourcePatientInfo -> context.sourcePatientInfo
                    // Reference(Patient) [0..1] Contained Patient Resource with
                    // Patient.identifier.use element set to ‘usual’.
                    Identifiable sourcePatientId = documentEntry.getSourcePatientId();
                    PatientInfo sourcePatientInfo = documentEntry.getSourcePatientInfo();
                    
                    Patient sourcePatient = new Patient();
                    if (sourcePatientId != null) {
                      sourcePatient.addIdentifier((new Identifier().setSystem(sourcePatientId.getAssigningAuthority().getUniversalId())
                            .setValue(sourcePatientId.getId())).setUse(IdentifierUse.OFFICIAL));
                    }
                    
                    if (sourcePatientInfo != null) {
	                    sourcePatient.setBirthDateElement(transformToDate(sourcePatientInfo.getDateOfBirth()));
	                    String gender = sourcePatientInfo.getGender();
                    }
                    
                    if (sourcePatientId != null || sourcePatientInfo != null) {
                      context.getSourcePatientInfo().setResource(sourcePatient);
                    }
                    
                }
            }
        } else {
           processError(input);
        }
        return list;
    }

}
