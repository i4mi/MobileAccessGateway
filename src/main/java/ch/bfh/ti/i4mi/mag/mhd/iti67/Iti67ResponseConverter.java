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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.MagConstants;
import ch.bfh.ti.i4mi.mag.mhd.BaseQueryResponseConverter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceRelatesToComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentRelationshipType;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Address;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.*;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.owasp.esapi.codecs.Hex;

import java.util.*;

/**
 * ITI-67 from ITI-18 response converter
 *
 * @author alexander kreutz
 */
public class Iti67ResponseConverter extends BaseQueryResponseConverter {

    public Iti67ResponseConverter(final Config config) {
        super(config);
    }

    @Override
    public List<DocumentReference> translateToFhir(QueryResponse input, Map<String, Object> parameters) {
        ArrayList<DocumentReference> list = new ArrayList<DocumentReference>();
        if (input != null && Status.SUCCESS.equals(input.getStatus())) {

            // process relationship association
            Map<String, List<DocumentReferenceRelatesToComponent>> relatesToMapping = new HashMap<String, List<DocumentReferenceRelatesToComponent>>();
            for (Association association : input.getAssociations()) {

                // Relationship type -> relatesTo.code code [1..1]
                // relationship reference -> relatesTo.target Reference(DocumentReference)

                String source = association.getSourceUuid();
                String target = association.getTargetUuid();
                AssociationType type = association.getAssociationType();

                DocumentReferenceRelatesToComponent relatesTo = new DocumentReferenceRelatesToComponent();
                if (type != null) switch (type) {
                    case APPEND:
                        relatesTo.setCode(DocumentRelationshipType.APPENDS);
                        break;
                    case REPLACE:
                        relatesTo.setCode(DocumentRelationshipType.REPLACES);
                        break;
                    case TRANSFORM:
                        relatesTo.setCode(DocumentRelationshipType.TRANSFORMS);
                        break;
                    case SIGNS:
                        relatesTo.setCode(DocumentRelationshipType.SIGNS);
                        break;
                }
                relatesTo.setTarget(new Reference().setReference("urn:oid:" + target));

                if (!relatesToMapping.containsKey(source))
                    relatesToMapping.put(source, new ArrayList<DocumentReferenceRelatesToComponent>());
                relatesToMapping.get(source).add(relatesTo);
            }


            if (input.getDocumentEntries() != null) {
                for (DocumentEntry documentEntry : input.getDocumentEntries()) {
                    DocumentReference documentReference = new DocumentReference();

                    list.add(documentReference);
                    // limitedMetadata -> meta.profile canonical [0..*] 
                    if (documentEntry.isLimitedMetadata()) {
                        documentReference.getMeta().addProfile(
                                "https://ihe.net/fhir/StructureDefinition/IHE_MHD_Query_Comprehensive_DocumentReference");
                    } else {
                        documentReference.getMeta().addProfile(
                                "https://ihe.net/fhir/StructureDefinition/IHE_MHD_Comprehensive_DocumentManifest");
                    }

                    // uniqueId -> masterIdentifier Identifier [0..1] [1..1]
                    if (documentEntry.getUniqueId() != null) {
                        documentReference.setMasterIdentifier(
                                (new Identifier().setValue("urn:oid:" + documentEntry.getUniqueId())).setSystem(
                                        "urn:ietf:rfc:3986"));
                    }

                    // entryUUID -> identifier Identifier [0..*]
                    // When the DocumentReference.identifier carries the entryUUID then the
                    // DocumentReference.identifier. use shall be ‘official’
                    if (documentEntry.getEntryUuid() != null) {
                        documentReference.addIdentifier((new Identifier().setSystem("urn:ietf:rfc:3986")
                                .setValue(asUuid(documentEntry.getEntryUuid()))).setUse(IdentifierUse.OFFICIAL));
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
                    if (documentEntry.getPatientId() != null) {
                        final Identifiable patient = documentEntry.getPatientId();
                        if (StringUtils.isNotBlank(this.config.getUriExternalPatientEndpoint())) {
                            final GenericClient client = new GenericClient(FhirContext.forR4Cached(), null,
                                                                           this.config.getUriExternalPatientEndpoint(),
                                                                           null);
                            client.setDontValidateConformance(true);
                            final var bundle = (Bundle) client.search()
                                    .forResource(Patient.class)
                                    .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:oid:" + patient.getAssigningAuthority().getUniversalId(),
                                                                                            patient.getId()))
                                    .returnBundle(Bundle.class)
                                    .execute();
                            if (!bundle.getEntry().isEmpty()) {
                                final var result = new Reference();
                                result.setReference(bundle.getEntry().get(0).getFullUrl());
                                documentReference.setSubject(result);
                            }
                        } else {
                            documentReference.setSubject(transformPatient(patient));
                        }
                    }

                    // creationTime -> date instant [0..1]
                    if (documentEntry.getCreationTime() != null) {
                        documentReference.setDate(Date.from(documentEntry.getCreationTime().getDateTime().toInstant()));
                    }

                    // authorPerson, authorInstitution, authorPerson, authorRole,
                    // authorSpeciality, authorTelecommunication -> author Reference(Practitioner|
                    // PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]                   
                    if (documentEntry.getAuthors() != null) {
                        for (Author author : documentEntry.getAuthors()) {
                            documentReference.addAuthor(transformAuthor(author));
                        }
                    }

                    // legalAuthenticator -> authenticator Note 1
                    // Reference(Practitioner|Practition erRole|Organization [0..1]
                    Person person = documentEntry.getLegalAuthenticator();
                    if (person != null) {
                        Practitioner practitioner = transformPractitioner(person);
                        documentReference.setAuthenticator((Reference) new Reference().setResource(practitioner));
                    }

                    // Relationship Association -> relatesTo [0..*]                   
                    // [1..1]                    
                    documentReference.setRelatesTo(relatesToMapping.get(documentEntry.getEntryUuid()));


                    // confidentialityCode -> securityLabel CodeableConcept [0..*] Note: This
                    // is NOT the DocumentReference.meta, as that holds the meta tags for the
                    // DocumentReference itself.
                    if (documentEntry.getConfidentialityCodes() != null) {
                        documentReference.addSecurityLabel(transform(documentEntry.getConfidentialityCodes()));
                    }

                    DocumentReferenceContentComponent content = documentReference.addContent();
                    Attachment attachment = new Attachment();
                    content.setAttachment(attachment);

                    // title -> content.attachment.title string [0..1]
                    if (documentEntry.getTitle() != null) {
                        attachment.setTitle(documentEntry.getTitle().getValue());
                    }


                    // mimeType -> content.attachment.contentType [1..1] code [0..1]
                    if (documentEntry.getMimeType() != null) {
                        attachment.setContentType(documentEntry.getMimeType());
                    }

                    // languageCode -> content.attachment.language code [0..1]
                    if (documentEntry.getLanguageCode() != null) {
                        attachment.setLanguage(documentEntry.getLanguageCode());
                    }

                    // retrievable location of the document -> content.attachment.url uri
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
                    if (documentEntry.getHash() != null) {
                        attachment.setHash(Hex.fromHex(documentEntry.getHash()));
                    }

                    // comments -> description string [0..1]
                    if (documentEntry.getComments() != null) {
                        documentReference.setDescription(documentEntry.getComments().getValue());
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

                    // referenceIdList -> context.encounter Reference(Encounter) [0..*] When
                    // referenceIdList contains an encounter, and a FHIR Encounter is available, it
                    // may be referenced.
                    // Map to context.related
                    List<ReferenceId> refIds = documentEntry.getReferenceIdList();
                    if (refIds != null) {
                        for (ReferenceId refId : refIds) {
                            context.getRelated().add(transform(refId));
                        }
                    }

                    // eventCodeList -> context.event CodeableConcept [0..*]
                    if (documentEntry.getEventCodeList() != null) {
                        documentReference.getContext().setEvent(transformMultiple(documentEntry.getEventCodeList()));
                    }

                    // serviceStartTime serviceStopTime -> context.period Period [0..1]
                    if (documentEntry.getServiceStartTime() != null || documentEntry.getServiceStopTime() != null) {
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

                    // sourcePatientId and sourcePatientInfo -> context.sourcePatientInfo
                    // Reference(Patient) [0..1] Contained Patient Resource with
                    // Patient.identifier.use element set to ‘usual’.
                    Identifiable sourcePatientId = documentEntry.getSourcePatientId();
                    PatientInfo sourcePatientInfo = documentEntry.getSourcePatientInfo();

                    Patient sourcePatient = new Patient();
                    if (sourcePatientId != null) {
                        sourcePatient.addIdentifier((new Identifier().setSystem("urn:oid:" + sourcePatientId.getAssigningAuthority().getUniversalId())
                                .setValue(sourcePatientId.getId())).setUse(IdentifierUse.OFFICIAL));
                    }

                    if (sourcePatientInfo != null) {
                        sourcePatient.setBirthDateElement(transformToDate(sourcePatientInfo.getDateOfBirth()));
                        String gender = sourcePatientInfo.getGender();
                        if (gender != null) {
                            switch (gender) {
                                case "F":
                                    sourcePatient.setGender(Enumerations.AdministrativeGender.FEMALE);
                                    break;
                                case "M":
                                    sourcePatient.setGender(Enumerations.AdministrativeGender.MALE);
                                    break;
                                case "U":
                                    sourcePatient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
                                    break;
                                case "A":
                                    sourcePatient.setGender(Enumerations.AdministrativeGender.OTHER);
                                    break;
                            }
                        }
                        ListIterator<Name> names = sourcePatientInfo.getNames();
                        while (names.hasNext()) {
                            Name name = names.next();
                            if (name != null) sourcePatient.addName(transform(name));
                        }
                        ListIterator<Address> addresses = sourcePatientInfo.getAddresses();
                        while (addresses.hasNext()) {
                            Address address = addresses.next();
                            if (address != null) sourcePatient.addAddress(transform(address));
                        }
                    }

                    if (sourcePatientId != null || sourcePatientInfo != null) {
                        context.getSourcePatientInfo().setResource(sourcePatient);
                    }

                    if (documentEntry.getExtraMetadata() != null) {
                        List<String> originalProviderRoles = documentEntry.getExtraMetadata().get(MagConstants.XdsExtraMetadataSlotNames.CH_ORIGINAL_PROVIDER_ROLE);
                        if (originalProviderRoles != null) {
                            for (String originalProviderRole : originalProviderRoles) {
                                Identifiable cx = Hl7v2Based.parse(originalProviderRole, Identifiable.class);
                                documentReference.addExtension(
                                        MagConstants.FhirExtensionUrls.CH_AUTHOR_ROLE,
                                        new Coding("urn:oid:" + cx.getAssigningAuthority().getUniversalId(), cx.getId(), null));
                            }
                        }

                        List<String> deletionStatuses = documentEntry.getExtraMetadata().get(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS);
                        if (deletionStatuses != null) {
                            for (String deletionStatus : deletionStatuses) {
                                documentReference.addExtension(
                                        MagConstants.FhirExtensionUrls.CH_DELETION_STATUS,
                                        new Coding("urn:oid:2.16.756.5.30.1.127.3.10.18", deletionStatus, null));
                            }
                        }
                    }

                    if (documentEntry.getRepositoryUniqueId() != null) {
                        documentReference.addExtension(
                                MagConstants.FhirExtensionUrls.REPOSITORY_UNIQUE_ID,
                                new Identifier().setSystem(MagConstants.FhirCodingSystemIds.RFC_3986).setValue("urn:oid:" + documentEntry.getRepositoryUniqueId()));
                    }

                    if (documentEntry.getVersion() != null) {
                        documentReference.addExtension(
                                MagConstants.FhirExtensionUrls.DOCUMENT_ENTRY_VERSION,
                                new PositiveIntType(documentEntry.getVersion().getVersionName()));
                    }

                    if (documentEntry.getAvailabilityStatus() != null) {
                        documentReference.addExtension(
                                MagConstants.FhirExtensionUrls.DOCUMENT_AVAILABILITY,
                                new Coding(MagConstants.FhirCodingSystemIds.RFC_3986, documentEntry.getDocumentAvailability().getFullQualified(), null));
                    }

                    String logicalId = (documentEntry.getLogicalUuid() != null) ? documentEntry.getLogicalUuid() : documentEntry.getEntryUuid();
                    if (logicalId != null) {
                        documentReference.addIdentifier()
                                .setValue(asUuid(logicalId))
                                .setSystem(MagConstants.FhirCodingSystemIds.RFC_3986)
                                .setType(new CodeableConcept().addCoding(new Coding(MagConstants.FhirCodingSystemIds.MHD_DOCUMENT_ID_TYPE, "logicalId", "Logical ID")));
                    }

                    documentReference.setId(noUuidPrefix(logicalId));
                }

            }
        } else {
            processError(input);
        }
        return list;
    }

}
