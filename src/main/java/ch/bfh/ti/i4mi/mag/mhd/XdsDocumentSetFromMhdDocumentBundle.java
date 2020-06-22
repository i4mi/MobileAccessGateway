package ch.bfh.ti.i4mi.mag.mhd;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.activation.DataHandler;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Document;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.PatientInfo;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Telecom;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.builder.ProvideAndRegisterDocumentSetBuilder;

import com.sun.istack.ByteArrayDataSource;

import ca.uhn.fhir.rest.param.DateParam;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XdsDocumentSetFromMhdDocumentBundle {

	public static ProvideAndRegisterDocumentSet convert(@Body Bundle requestBundle) {
		
		SubmissionSet submissionSet = new SubmissionSet();
		
		ProvideAndRegisterDocumentSetBuilder builder = new ProvideAndRegisterDocumentSetBuilder(true, submissionSet);
		
		Map<String, Resource> resources = new HashMap<String, Resource>();
		DocumentManifest manifest = null;
		for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
            Resource resource = requestEntry.getResource();
            if (resource instanceof DocumentManifest) {
            	manifest = (DocumentManifest) resource;            	
            } else if (resource instanceof DocumentReference) {
            	resources.put(requestEntry.getFullUrl(), resource);
            	
            } else if (resource instanceof ListResource) {
            	resources.put(requestEntry.getFullUrl(), resource);
            } else if (resource instanceof Binary) {
            	resources.put(requestEntry.getFullUrl(), resource);
            } else {
                throw new IllegalArgumentException(resource + " is not allowed here");
            } 			
        }
				
		processDocumentManifest(manifest, submissionSet);
		for (Reference content : manifest.getContent()) {
			String refTarget = content.getReference();
			Resource resource = resources.get(refTarget);
			if (resource instanceof DocumentReference) {
				DocumentReference documentReference = (DocumentReference) resource;
				Document doc = new Document();            	
        		DocumentEntry entry = new DocumentEntry();        		        		        		
                processDocumentReference((DocumentReference) resource, entry);
                doc.setDocumentEntry(entry);
                
                String contentURL = documentReference.getContentFirstRep().getAttachment().getUrl();
                Resource binaryContent = resources.get(contentURL);
                if (binaryContent instanceof Binary) {
                	Binary binary = (Binary) binaryContent;
                	doc.setDataHandler(new DataHandler(new ByteArrayDataSource(binary.getData(),binary.getContentType())));
                }
                
                builder.withDocument(doc);
			}
		}
		
		return builder.build();
	}
	
	public static Code transformCodeableConcept(CodeableConcept cc) {
		Coding coding = cc.getCodingFirstRep();
		return new Code(coding.getCode(), new LocalizedString(coding.getDisplay()), coding.getSystem());
	}
	
	public static Timestamp timestampFromDate(DateTimeType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("-","");
    	log.info(dateString);
    	return Timestamp.fromHL7(dateString);
    }
	
	public static Code transform(Coding coding) {
		if (coding==null) return null;
		return new Code(coding.getCode(), new LocalizedString(coding.getDisplay()), coding.getSystem());
	}
	
	public static Code transform(CodeableConcept cc) {
		if (cc==null) return null;
		Coding coding = cc.getCodingFirstRep();
		return transform(coding);
	}
	
	public static Code transform(List<CodeableConcept> ccs) {
		if (ccs==null || ccs.isEmpty()) return null;
		return transform(ccs.get(0));
	}
	
	public static Identifiable transformReferenceToIdentifiable(Reference reference, DomainResource container) {
		String targetRef = reference.getReference();		
		List<Resource> resources = container.getContained();		
		for (Resource resource : resources) {			
			if (targetRef.equals(resource.getId())) {
				Identifier identifier = ((Patient) resource).getIdentifierFirstRep();
				String system = identifier.getSystem();
		    	if (system.startsWith("urn:oid:")) {
		            system = system.substring(8);
		        }
				return new Identifiable(identifier.getValue(), new AssigningAuthority(system));
			}
		}
		
		String system = reference.getIdentifier().getSystem();
    	if (system.startsWith("urn:oid:")) {
            system = system.substring(8);
        }
    	String value = reference.getIdentifier().getValue();
    	
        return new Identifiable(value, new AssigningAuthority(system));
	}
	
	private static void processDocumentManifest(DocumentManifest manifest, SubmissionSet submissionSet) {
		// masterIdentifier	SubmissionSet.uniqueId
		Identifier masterIdentifier = manifest.getMasterIdentifier();
		submissionSet.setUniqueId(masterIdentifier.getValue());
		   
		// TODO
		//manifest.getIdentifier();
		//submissionSet.setEntryUuid(entryUuid);
		
		CodeableConcept type = manifest.getType();
		submissionSet.setContentTypeCode(transformCodeableConcept(type));
		
		DateTimeType created = manifest.getCreatedElement();
		submissionSet.setSubmissionTime(timestampFromDate(created));
		   
		//  subject	SubmissionSet.patientId
		Reference ref = manifest.getSubject();
		submissionSet.setPatientId(transformReferenceToIdentifiable(ref, manifest));
		
		// TODO Author
		//List<Reference> authors = manifest.getAuthor();
		//Author author = new Author();		
		//submissionSet.setAuthor(author);
		   				  
		 // TODO recipient	SubmissionSet.intendedRecipient
		//List<Reference> recipients = manifest.getRecipient();
		//submissionSet.set
		
		// source	SubmissionSet.sourceId
		String source = manifest.getSource();
		submissionSet.setSourceId(source);
		  
		String description = manifest.getDescription();
		submissionSet.setTitle(new LocalizedString(description));		  
		
	}
	
	private static void processDocumentReference(DocumentReference reference, DocumentEntry entry) {
		 // FIXME String uuid = UUID.randomUUID().toString();
		entry.setEntryUuid(reference.getId());
        
        
        // limitedMetadata -> meta.profile canonical [0..*] TODO
        // uniqueId -> masterIdentifier Identifier [0..1] [1..1]
        //if (documentEntry.getUniqueId() != null) {
        //    documentReference.setMasterIdentifier(
        //            (new Identifier().setValue("urn:oid:" + documentEntry.getUniqueId())));
        //}

        // entryUUID -> identifier Identifier [0..*]
        // When the DocumentReference.identifier carries the entryUUID then the
        // DocumentReference.identifier. use shall be ‘official’
        //if (documentEntry.getEntryUuid() != null) {
        //    documentReference.addIdentifier((new Identifier().setSystem("urn:ietf:rfc:3986")
        //            .setValue("urn:uuid:" + documentEntry.getEntryUuid())).setUse(IdentifierUse.OFFICIAL));
        //}
        // availabilityStatus -> status code {DocumentReferenceStatus} [1..1]
        // approved -> status=current
        // deprecated -> status=superseded
        // Other status values are allowed but are not defined in this mapping to XDS.
		DocumentReferenceStatus status = reference.getStatus();
		switch (status) {
		case CURRENT:entry.setAvailabilityStatus(AvailabilityStatus.APPROVED);break;
		case SUPERSEDED:entry.setAvailabilityStatus(AvailabilityStatus.DEPRECATED);break;
		default: // TODO throw error
		}
		
		// contentTypeCode -> type CodeableConcept [0..1]
		CodeableConcept type = reference.getType();
		entry.setTypeCode(transform(type));
		
		// classCode -> category CodeableConcept [0..*]
		List<CodeableConcept> category = reference.getCategory();
		entry.setClassCode(transform(category));
               
        // patientId -> subject Reference(Patient| Practitioner| Group| Device) [0..1],       
		Reference subject = reference.getSubject();
		entry.setPatientId(transformReferenceToIdentifiable(subject, reference));
		

        // creationTime -> date instant [0..1]
        //if (documentEntry.getCreationTime() != null) {
        //    documentReference.setDate(Date.from(documentEntry.getCreationTime().getDateTime().toInstant()));
        //}

        // TODO: authorPerson, authorInstitution, authorPerson, authorRole,
        // authorSpeciality, authorTelecommunication -> author Reference(Practitioner|
        // PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]                   
        //if (documentEntry.getAuthors() != null) {
        //}
        // TODO: legalAuthenticator -> authenticator Note 1
        // Reference(Practitioner|Practition erRole|Organization [0..1]
        //Person person = documentEntry.getLegalAuthenticator();
        //if (person != null) {
        //   Practitioner practitioner = transformPractitioner(person);
        //   documentReference.addContained(practitioner);
        //   documentReference.setAuthenticator(new Reference().setReference(practitioner.getId()));
        //}
        
        // TODO: Relationship Association -> relatesTo [0..*]                   
        // TODO: Relationship type -> relatesTo.code code [1..1]
        // TODO: relationship reference -> relatesTo.target Reference(DocumentReference)
        // [1..1]

        // title -> description string [0..1]
        //if (documentEntry.getTitle() != null) {
        //    documentReference.setDescription(documentEntry.getTitle().getValue());
        //}

        // confidentialityCode -> securityLabel CodeableConcept [0..*] Note: This
        // is NOT the DocumentReference.meta, as that holds the meta tags for the
        // DocumentReference itself.
        //if (documentEntry.getConfidentialityCodes() != null) {
        //    documentReference.addSecurityLabel(transform(documentEntry.getConfidentialityCodes()));
        //}

        //DocumentReferenceContentComponent content = documentReference.addContent();
        //Attachment attachment = new Attachment();
        //content.setAttachment(attachment);

        // mimeType -> content.attachment.contentType [1..1] code [0..1]
		DocumentReferenceContentComponent content = reference.getContentFirstRep();		
		if (content==null) throw new NullPointerException(); // TODO throw error
		Attachment attachment = content.getAttachment();
		if (attachment==null) throw new NullPointerException(); // TODO throw error
		entry.setMimeType(attachment.getContentType());
		       
        // languageCode -> content.attachment.language code [0..1]
		entry.setLanguageCode(attachment.getLanguage());

        // size -> content.attachment.size integer [0..1] The size is calculated
        entry.setSize((long) attachment.getSize());


        
        // on the data prior to base64 encoding, if the data is base64 encoded.
        // TODO: hash -> content.attachment.hash string [0..1]
        //if (documentEntry.getHash()!=null) {
        //	attachment.setHash(documentEntry.getHash().getBytes());
        //}

        // comments -> content.attachment.title string [0..1]
        //if (documentEntry.getComments() != null) {
        //    attachment.setTitle(documentEntry.getComments().getValue());
        //}

        // TcreationTime -> content.attachment.creation dateTime [0..1]
        //if (documentEntry.getCreationTime() != null) {
        //    attachment.setCreation(Date.from(documentEntry.getCreationTime().getDateTime().toInstant()));
        //}

        // formatCode -> content.format Coding [0..1]
        Coding coding = content.getFormat();
        entry.setFormatCode(transform(coding));       

        //DocumentReferenceContextComponent context = new DocumentReferenceContextComponent();
        //documentReference.setContext(context);

        // TODO: referenceIdList -> context.encounter Reference(Encounter) [0..*] When
        // referenceIdList contains an encounter, and a FHIR Encounter is available, it
        // may be referenced.
        //List<ReferenceId> refIds = documentEntry.getReferenceIdList();
        //if (refIds!=null) {
        //	for (ReferenceId refId : refIds) {
        //		if (ReferenceId.ID_TYPE_ENCOUNTER_ID.equals(refId.getIdTypeCode())) {
        //			context.addEncounter(transform(refId));
        //		}
        //	}
        //}
        
        // eventCodeList -> context.event CodeableConcept [0..*]
        //if (documentEntry.getEventCodeList()!=null) {
        //	documentReference.getContext().setEvent(transformMultiple(documentEntry.getEventCodeList()));
        //}
        
        // serviceStartTime serviceStopTime -> context.period Period [0..1]
        //if (documentEntry.getServiceStartTime()!=null || documentEntry.getServiceStopTime()!=null) {
        //	Period period = new Period();
        //	period.setStartElement(transform(documentEntry.getServiceStartTime()));
        //	period.setEndElement(transform(documentEntry.getServiceStopTime()));
        //	documentReference.getContext().setPeriod(period);
        //}

        // healthcareFacilityTypeCode -> context.facilityType CodeableConcept
        // [0..1]
        //if (documentEntry.getHealthcareFacilityTypeCode() != null) {
        //    context.setFacilityType(transform(documentEntry.getHealthcareFacilityTypeCode()));
        //}

        // practiceSettingCode -> context.practiceSetting CodeableConcept [0..1]
        //if (documentEntry.getPracticeSettingCode() != null) {
         //   context.setPracticeSetting(transform(documentEntry.getPracticeSettingCode()));
        //}

        // TODO: sourcePatientId and sourcePatientInfo -> context.sourcePatientInfo
        // Reference(Patient) [0..1] Contained Patient Resource with
        // Patient.identifier.use element set to ‘usual’.
        //Identifiable sourcePatientId = documentEntry.getSourcePatientId();
        //PatientInfo sourcePatientInfo = documentEntry.getSourcePatientInfo();___________________________________________________________________________ ___________________________________________________________________________ 52 Rev. 3.1 – 2019-03-06 Copyright © 2019: IHE International, Inc.FHIR DocumentReference Resource DefinitionIHE constraintDocument Sharing MetadataNotesmeta.source uri [0..1]Allowed but not defined Note 3meta.profile canonical [0..*]limitedMetadataSee Section 4.5.1.1.1.meta.security Coding [0..*]Allowed but not defined Note 3meta.tag Coding [0..*]Allowed but not defined Note 3implicitRules uri [0..1]Allowed but not defined Note 3language code [0..1]Allowed but not defined Note 3text Narrative [0..1]Allowed but not defined Note 3contained Resource [0..*]Allowed but not defined Note 3extension [0..*]Allowed but not defined Note 3modifierExtension Extension [0..*]Allowed but not defined Note 3masterIdentifierIdentifier [0..1] [1..1]uniqueIdSee ITI  TF-2x: Z.9.1.1 Identifier and CDA root plus extensionidentifierIdentifier [0..*]entryUUIDWhen the DocumentReference.identifier carries the entryUUID then the DocumentReference.identifier.use shall be ‘official’statuscode {DocumentReferenceStatus} [1..1]availabilityStatusapproved  status=currentdeprecated status=supersededOther status values are allowed but are not defined in thismapping to XDS. docStatuscode [0..1]Allowed but not defined Note 3type CodeableConcept [0..1]typeCodecategoryCodeableConcept [0..*] [0..1]classCode 
	}
	
	
}
