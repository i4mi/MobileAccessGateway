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

package ch.bfh.ti.i4mi.mag.mhd.iti65;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceRelatesToComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentRelationshipType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Document;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Name;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.PatientInfo;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Recipient;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Telecom;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.XpnName;
import org.openehealth.ipf.commons.ihe.xds.core.requests.ProvideAndRegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.builder.ProvideAndRegisterDocumentSetBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.sun.istack.ByteArrayDataSource;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * ITI-65 to ITI-41 request converter
 * @author alexander kreutz
 *
 */
@Slf4j
public class Iti65RequestConverter {

	private SchemeMapper schemeMapper;
	
	@Autowired
	public void setSchemeMapper(SchemeMapper schemeMapper) {
		this.schemeMapper = schemeMapper;
	}
	
	/**
	 * convert ITI-65 to ITI-41 request
	 * @param requestBundle
	 * @return
	 */
	public ProvideAndRegisterDocumentSet convert(@Body Bundle requestBundle) {
		
		SubmissionSet submissionSet = new SubmissionSet();
		
		ProvideAndRegisterDocumentSetBuilder builder = new ProvideAndRegisterDocumentSetBuilder(true, submissionSet);
		
		// create mapping fullUrl -> resource for each resource in bundle
		Map<String, Resource> resources = new HashMap<String, Resource>();
		DocumentManifest manifest = null;
		for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
            Resource resource = requestEntry.getResource();
            if (resource instanceof DocumentManifest) {
            	manifest = (DocumentManifest) resource;            	
            } else if (resource instanceof DocumentReference) {
            	resources.put(requestEntry.getFullUrl(), resource);
            	
            } else if (resource instanceof ListResource) {
            	new IllegalArgumentException("List Resource is currently not supported");
            	//resources.put(requestEntry.getFullUrl(), resource);
            } else if (resource instanceof Binary) {
            	resources.put(requestEntry.getFullUrl(), resource);
            } else {
                throw new IllegalArgumentException(resource + " is not allowed here");
            } 			
        }
						
		processDocumentManifest(manifest, submissionSet);
		
		// set limited metadata
		for (CanonicalType profile : requestBundle.getMeta().getProfile()) {
			if ("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(false);
			} else if ("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(true);
			}
		}
		
		// process all resources referenced in DocumentManifest.content
		for (Reference content : manifest.getContent()) {
			String refTarget = content.getReference();
			Resource resource = resources.get(refTarget);
			if (resource instanceof DocumentReference) {
				DocumentReference documentReference = (DocumentReference) resource;
				Document doc = new Document();            	
        		DocumentEntry entry = new DocumentEntry();        		        		        		
                processDocumentReference(documentReference, entry);
                doc.setDocumentEntry(entry);
                
                // create associations
                for (DocumentReferenceRelatesToComponent relatesTo : documentReference.getRelatesTo()) {
                	Reference target = relatesTo.getTarget();
                	DocumentRelationshipType code = relatesTo.getCode();
                	Association association = new Association();
                	switch(code) {
                	case REPLACES:association.setAssociationType(AssociationType.REPLACE);break; 
                	case TRANSFORMS:association.setAssociationType(AssociationType.TRANSFORM);break; 
                	case SIGNS:association.setAssociationType(AssociationType.SIGNS);break; 
                	case APPENDS:association.setAssociationType(AssociationType.APPEND);break;
                	default:
                	}
                	association.setSourceUuid(entry.getEntryUuid());
                	association.setTargetUuid(transformUriFromReference(target));
                	
                	builder.withAssociation(association);
                }
                
                // get binary content from attachment.data or from referenced Binary resource
                Attachment attachment = documentReference.getContentFirstRep().getAttachment();
                if (attachment.hasData()) {
                	doc.setDataHandler(new DataHandler(new ByteArrayDataSource(attachment.getData(),attachment.getContentType())));
                } else if (attachment.hasUrl()) {
                    String contentURL = attachment.getUrl();                
	                Resource binaryContent = resources.get(contentURL);
	                if (binaryContent instanceof Binary) {
	                	Binary binary = (Binary) binaryContent;	                	
	                	doc.setDataHandler(new DataHandler(new ByteArrayDataSource(binary.getData(),binary.getContentType())));	            
                    }
                }
                
                builder.withDocument(doc);
			}
		}
		
		return builder.build();
	}
	
	/**
	 * wrap string in localized string
	 * @param string
	 * @return
	 */
	public LocalizedString localizedString(String string) {
		if (string==null) return null;
		return new LocalizedString(string);
	}
	
	/**
	 * FHIR CodeableConcept -> XDS Code
	 * @param cc
	 * @return
	 */
	public  Code transformCodeableConcept(CodeableConcept cc) {
		if (cc == null) return null;
		if (!cc.hasCoding()) return null;
		Coding coding = cc.getCodingFirstRep();
		return new Code(coding.getCode(), localizedString(coding.getDisplay()), schemeMapper.getScheme(coding.getSystem()));
	}
	
	/**
	 * FHIR CodeableConcept list -> XDS code list
	 * @param ccs
	 * @param target
	 */
	public  void transformCodeableConcepts(List<CodeableConcept> ccs, List<Code> target) {
		if (ccs == null || ccs.isEmpty()) return;
		for (CodeableConcept cc : ccs) {
			Code code = transformCodeableConcept(cc); 
			if (code!=null) target.add(code);
		}
	}
	
	/**
	 * FHIR DateType -> XDS Timestamp
	 * @param date
	 * @return
	 */
	public  Timestamp timestampFromDate(DateType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("[T\\-:]","");    	
    	return Timestamp.fromHL7(dateString);
    }
	
	/**
	 * FHIR DateTimeType -> XDS Timestamp
	 * @param date
	 * @return
	 */
	public  Timestamp timestampFromDate(DateTimeType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("[T\\-:]","");    	
    	return Timestamp.fromHL7(dateString);
    }
	
	/**
	 * FHIR InstantType -> XDS Timestamp
	 * @param date
	 * @return
	 */
	public  Timestamp timestampFromDate(InstantType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("[T\\-:]","");    	
    	return Timestamp.fromHL7(dateString);
    }
	
	/**
	 * FHIR Coding -> XDS Code
	 * @param coding
	 * @return
	 */
	public  Code transform(Coding coding) {
		if (coding==null) return null;
		return new Code(coding.getCode(), localizedString(coding.getDisplay()), schemeMapper.getScheme(coding.getSystem()));
	}
	
	/**
	 * FHIR CodeableConcept -> XDS Code
	 * @param cc
	 * @return
	 */
	public  Code transform(CodeableConcept cc) {
		if (cc==null) return null;
		Coding coding = cc.getCodingFirstRep();
		return transform(coding);
	}
	
	/**
	 * FHIR CodeableConcept list -> XDS code
	 * @param ccs
	 * @return
	 */
	public  Code transform(List<CodeableConcept> ccs) {
		if (ccs==null || ccs.isEmpty()) return null;
		return transform(ccs.get(0));
	}
	
	/**
	 * FHIR CodeableConcept -> XDS Identifiable
	 * @param cc
	 * @return
	 */
	public Identifiable transformToIdentifiable(CodeableConcept cc) {
		Code code = transform(cc);
		String system = code.getSchemeName();			    	
		return new Identifiable(code.getCode(), new AssigningAuthority(system));
	}
	
	/**
	 * FHIR Address -> XDS Address
	 * @param address
	 * @return
	 */
	public org.openehealth.ipf.commons.ihe.xds.core.metadata.Address transform(Address address) {
		org.openehealth.ipf.commons.ihe.xds.core.metadata.Address targetAddress = new org.openehealth.ipf.commons.ihe.xds.core.metadata.Address();
		    
		targetAddress.setCity(address.getCity());
		targetAddress.setCountry(address.getCountry());
		targetAddress.setStateOrProvince(address.getState());
		targetAddress.setZipOrPostalCode(address.getPostalCode());
		String streetAddress = null; 
		for (StringType street : address.getLine()) {
		  	if (streetAddress == null) streetAddress = street.getValue();
		   	else streetAddress += "\n"+street.getValue();
		}
		targetAddress.setStreetAddress(streetAddress);
		return targetAddress;
	}
	
	/**
	 * remove "urn:oid:" prefix from code system
	 * @param system
	 * @return
	 */
	public String noPrefix(String system) {
		if (system == null) return null;
		if (system.startsWith("urn:oid:")) {
            system = system.substring(8);
        }
		return system;
	}
	
	/**
	 * FHIR Identifier -> XDS Identifiable
	 * @param identifier
	 * @return
	 */
	public Identifiable transform(Identifier identifier) {
		String system = noPrefix(identifier.getSystem());			    	
		return new Identifiable(identifier.getValue(), new AssigningAuthority(system));
	}
	
	/**
	 * FHIR Reference -> XDS Identifiable
	 * Only for References to Patients or Encounters
	 * Identifier is extracted from contained resource or from Reference URL
	 * @param reference
	 * @param container
	 * @return
	 */
	public  Identifiable transformReferenceToIdentifiable(Reference reference, DomainResource container) {
		if (reference.hasReference()) {
			String targetRef = reference.getReference();		
			List<Resource> resources = container.getContained();		
			for (Resource resource : resources) {			
				if (targetRef.equals(resource.getId())) {
					if (resource instanceof Patient) {
					  return transform(((Patient) resource).getIdentifierFirstRep());
					} else if (resource instanceof Encounter) {
						return transform(((Encounter) resource).getIdentifierFirstRep());
					}
				}
			}
			MultiValueMap<String, String> vals = UriComponentsBuilder.fromUriString(targetRef).build().getQueryParams();
			if (vals.containsKey("identifier")) {
				String[] identifier = vals.getFirst("identifier").split("\\|");
				if (identifier.length == 2) {
					return new Identifiable(identifier[1], new AssigningAuthority(noPrefix(identifier[0])));
				}
			}
		} else if (reference.hasIdentifier()) {					
	        return transform(reference.getIdentifier());
		} 
		throw new InvalidRequestException("Cannot resolve patient reference");
	}
	
	/**
	 * FHIR Reference to Patient -> XDS PatientInfo
	 * @param ref
	 * @param container
	 * @return
	 */
	public PatientInfo transformReferenceToPatientInfo(Reference ref, DomainResource container) {
		if (ref == null) return null;
		if (!ref.hasReference()) return null;
		List<Resource> resources = container.getContained();		
		for (Resource resource : resources) {	
			String targetRef = ref.getReference();
		
			if (targetRef.equals(resource.getId())) {
				Patient patient = ((Patient) resource);
				
				PatientInfo patientInfo = new PatientInfo();
				patientInfo.setDateOfBirth(timestampFromDate(patient.getBirthDateElement()));
				Enumerations.AdministrativeGender gender = patient.getGender();
				if (gender != null) {
					switch(gender) {
					case MALE: patientInfo.setGender("M");break;
					case FEMALE: patientInfo.setGender("F");break;
					case OTHER: patientInfo.setGender("A");break;
					default: patientInfo.setGender("U");break;
					}
				}
				
				for (HumanName name : patient.getName()) {					
					patientInfo.getNames().add(transform(name));	
				}
				
				for (Address address : patient.getAddress()) {				    
				    patientInfo.getAddresses().add(transform(address));
				}
				
				for (Identifier id : patient.getIdentifier()) {
				  patientInfo.getIds().add(transform(id));
				}
				
				return patientInfo;
			}
		}
		return null;
	}
	
	/**
	 * FHIR Reference -> URI String
	 * @param ref
	 * @return
	 */
	private String transformUriFromReference(Reference ref) {
		if (ref.hasIdentifier()) {
			return ref.getIdentifier().getValue();
		}
		return noPrefix(ref.getReference());
	}
	
	/**
	 * ITI-65: process DocumentManifest resource from Bundle
	 * @param manifest
	 * @param submissionSet
	 */
	private  void processDocumentManifest(DocumentManifest manifest, SubmissionSet submissionSet) {
		// masterIdentifier	SubmissionSet.uniqueId
		Identifier masterIdentifier = manifest.getMasterIdentifier();
		submissionSet.setUniqueId(noPrefix(masterIdentifier.getValue()));
		   
		
		submissionSet.assignEntryUuid();
		manifest.setId(submissionSet.getEntryUuid());
				
		CodeableConcept type = manifest.getType();
		submissionSet.setContentTypeCode(transformCodeableConcept(type));
		
		DateTimeType created = manifest.getCreatedElement();
		submissionSet.setSubmissionTime(timestampFromDate(created));
		   
		//  subject	SubmissionSet.patientId
		Reference ref = manifest.getSubject();
		submissionSet.setPatientId(transformReferenceToIdentifiable(ref, manifest));
		
		// Author
		if (manifest.hasAuthor()) {
			Reference author = manifest.getAuthorFirstRep();
			submissionSet.setAuthor(transformAuthor(author, manifest.getContained()));
		}
				   				 
		 // recipient	SubmissionSet.intendedRecipient		
		for (Reference recipientRef : manifest.getRecipient()) {
			Resource res = findResource(recipientRef, manifest.getContained());
			
			if (res instanceof Practitioner) {
				Recipient recipient = new Recipient();
				recipient.setPerson(transform((Practitioner) res));
				recipient.setTelecom(transform(((Practitioner) res).getTelecomFirstRep()));
				submissionSet.getIntendedRecipients().add(recipient);
			} else if (res instanceof Organization) {
				Recipient recipient = new Recipient();
				recipient.setOrganization(transform((Organization) res));
				recipient.setTelecom(transform(((Organization) res).getTelecomFirstRep()));
				submissionSet.getIntendedRecipients().add(recipient);
			} else if (res instanceof PractitionerRole) {
				Recipient recipient = new Recipient();
				PractitionerRole role = (PractitionerRole) res;
				recipient.setOrganization(transform((Organization) findResource(role.getOrganization(), manifest.getContained())));
				recipient.setPerson(transform((Practitioner) findResource(role.getPractitioner(), manifest.getContained())));
				recipient.setTelecom(transform(role.getTelecomFirstRep()));
				submissionSet.getIntendedRecipients().add(recipient);
			} else if (res instanceof Patient) {
				Recipient recipient = new Recipient();
				recipient.setPerson(transform((Patient) res));
				recipient.setTelecom(transform(((Patient) res).getTelecomFirstRep()));
			} else if (res instanceof RelatedPerson) {
				Recipient recipient = new Recipient();
				recipient.setPerson(transform((RelatedPerson) res));
				recipient.setTelecom(transform(((RelatedPerson) res).getTelecomFirstRep()));
			}								
						
		}
		
		// source	SubmissionSet.sourceId
		String source = noPrefix(manifest.getSource());		
		submissionSet.setSourceId(source);
		  
		String description = manifest.getDescription();		
		if (description!=null) submissionSet.setTitle(localizedString(description));		  
		
	}
	
	/**
	 * ITI-65: process DocumentReference resource from Bundle
	 * @param reference
	 * @param entry
	 */
	private  void processDocumentReference(DocumentReference reference, DocumentEntry entry) {
		
        entry.assignEntryUuid();
        reference.setId(entry.getEntryUuid());
        
        // limitedMetadata -> meta.profile canonical [0..*] 
        // No action

        
        // availabilityStatus -> status code {DocumentReferenceStatus} [1..1]
        // approved -> status=current
        // deprecated -> status=superseded
        // Other status values are allowed but are not defined in this mapping to XDS.
		DocumentReferenceStatus status = reference.getStatus();
		switch (status) {
		case CURRENT:entry.setAvailabilityStatus(AvailabilityStatus.APPROVED);break;
		case SUPERSEDED:entry.setAvailabilityStatus(AvailabilityStatus.DEPRECATED);break;
		default: throw new InvalidRequestException("Unknown document status");
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
		entry.setCreationTime(timestampFromDate(reference.getDateElement()));

        // authorPerson, authorInstitution, authorPerson, authorRole,
        // authorSpeciality, authorTelecommunication -> author Reference(Practitioner|
        // PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]   		
		for (Reference authorRef : reference.getAuthor()) {
		   entry.getAuthors().add(transformAuthor(authorRef, reference.getContained()));
		}
		
        // legalAuthenticator -> authenticator Note 1
		Reference authenticatorRef = reference.getAuthenticator();
		if (authenticatorRef!=null) {
			 Resource authenticator = findResource(authenticatorRef, reference.getContained());
			 if (authenticator instanceof Practitioner) {
				 entry.setLegalAuthenticator(transform((Practitioner) authenticator));	
			 } else if (authenticator instanceof PractitionerRole) {
				 Practitioner practitioner = (Practitioner) findResource(((PractitionerRole) authenticator).getPractitioner(), reference.getContained());
				 if (practitioner != null) entry.setLegalAuthenticator(transform((Practitioner) authenticator));
			 } 
			 // TODO What shall be done with an Organization authenticator?
		}
		             
        // title -> description string [0..1]
		String title = reference.getDescription();
		if (title != null) entry.setTitle(localizedString(title));
       
        // confidentialityCode -> securityLabel CodeableConcept [0..*] Note: This
        // is NOT the DocumentReference.meta, as that holds the meta tags for the
        // DocumentReference itself.		
		List<CodeableConcept> securityLabels = reference.getSecurityLabel();
		transformCodeableConcepts(securityLabels, entry.getConfidentialityCodes());				       
      
        // mimeType -> content.attachment.contentType [1..1] code [0..1]
		DocumentReferenceContentComponent content = reference.getContentFirstRep();		
		if (content==null) throw new InvalidRequestException("Missing content field in DocumentReference");
		Attachment attachment = content.getAttachment();
		if (attachment==null) throw new InvalidRequestException("Missing attachment field in DocumentReference");
		entry.setMimeType(attachment.getContentType());
		       
        // languageCode -> content.attachment.language code [0..1]
		entry.setLanguageCode(attachment.getLanguage());

        // size -> content.attachment.size integer [0..1] The size is calculated
		if (attachment.hasSize()) entry.setSize((long) attachment.getSize());
        
        // on the data prior to base64 encoding, if the data is base64 encoded.
        // hash -> content.attachment.hash string [0..1]
        byte[] hash = attachment.getHash();
        if (hash != null) entry.setHash(new String(hash));

        // comments -> content.attachment.title string [0..1]
        String comments = attachment.getTitle();
        if (comments!=null) entry.setComments(localizedString(comments));       

        // TcreationTime -> content.attachment.creation dateTime [0..1]
        // TODO is this a duplicate?
        //entry.setCreationTime(timestampFromDate(attachment.getCreationElement()));

        // formatCode -> content.format Coding [0..1]
        Coding coding = content.getFormat();
        entry.setFormatCode(transform(coding));       

        DocumentReferenceContextComponent context = reference.getContext();
       
        // TODO: referenceIdList -> context.encounter Reference(Encounter) [0..*] When
        // referenceIdList contains an encounter, and a FHIR Encounter is available, it
        // may be referenced.
        // Currently not mapped
        /*for (Reference encounterRef : context.getEncounter()) {
        	ReferenceId referenceId = new ReferenceId();
        	Identifiable id = transformReferenceToIdentifiable(encounterRef, reference);
        	if (id != null) {
        	  referenceId.setIdTypeCode(ReferenceId.ID_TYPE_ENCOUNTER_ID);        	
        	  referenceId.setId(id.getId());
        	  //referenceId.setAssigningAuthority(new CXiAid.getAssigningAuthority().getUniversalId());
			  entry.getReferenceIdList().add(referenceId );
        }*/
        
        // eventCodeList -> context.event CodeableConcept [0..*]       
        List<CodeableConcept> events = context.getEvent();
        transformCodeableConcepts(events, entry.getEventCodeList());
                
        // serviceStartTime serviceStopTime -> context.period Period [0..1]
        Period period = context.getPeriod();
        if (period != null) {
        	entry.setServiceStartTime(timestampFromDate(period.getStartElement()));
        	entry.setServiceStopTime(timestampFromDate(period.getEndElement()));
        }
        
        // healthcareFacilityTypeCode -> context.facilityType CodeableConcept
        // [0..1]
        entry.setHealthcareFacilityTypeCode(transformCodeableConcept(context.getFacilityType()));
        
        // practiceSettingCode -> context.practiceSetting CodeableConcept [0..1]
        entry.setPracticeSettingCode(transformCodeableConcept(context.getPracticeSetting()));
        
       
        // sourcePatientId and sourcePatientInfo -> context.sourcePatientInfo
        // Reference(Patient) [0..1] Contained Patient Resource with
        // Patient.identifier.use element set to ‘usual’.
        if (context.hasSourcePatientInfo()) {
          entry.setSourcePatientId(transformReferenceToIdentifiable(context.getSourcePatientInfo(), reference));
          entry.setSourcePatientInfo(transformReferenceToPatientInfo(context.getSourcePatientInfo(), reference));
        }
                  
	}
	
	/**
	 * search a referenced resource from a list of (contained) resources.
	 * @param ref
	 * @param contained
	 * @return
	 */
	public Resource findResource(Reference ref, List<Resource> contained) {
		for (Resource res : contained) {
			if (res.getId().equals(ref.getReference())) return res;
		}
		return null;
	}
	
	/**
	 * FHIR HumanName -> XDS Name
	 * @param name
	 * @return
	 */
	public Name transform(HumanName name) {
		Name targetName = new XpnName();
		if (name.hasPrefix()) targetName.setPrefix(name.getPrefixAsSingleString());
		if (name.hasSuffix()) targetName.setSuffix(name.getSuffixAsSingleString());
		targetName.setFamilyName(name.getFamily());
		List<StringType> given = name.getGiven();
		if (given != null && !given.isEmpty()) {
		   targetName.setGivenName(given.get(0).getValue());
		   if (given.size()>1) {						   
		       StringBuffer restOfName = new StringBuffer();
		       for (int part=1;part<given.size();part++) {					    	   
		           if (part > 1) restOfName.append(" ");
		    	   restOfName.append(given.get(part).getValue());					    	   
		       }
			   targetName.setSecondAndFurtherGivenNames(restOfName.toString());
		   }
		}
		return targetName;		
	}
	
	/**
	 * FHIR Practitioner -> XDS Person
	 * @param practitioner
	 * @return
	 */
	public Person transform(Practitioner practitioner) {
		if (practitioner == null) return null;
	   Person result = new Person();
	   if (practitioner.hasName()) result.setName(transform(practitioner.getNameFirstRep()));
	   result.setId(transform(practitioner.getIdentifierFirstRep()));
	   return result;
	}
	
	/**
	 * FHIR Patient -> XDS Person
	 * @param patient
	 * @return
	 */
	public Person transform(Patient patient) {
		if (patient == null) return null;
	   Person result = new Person();
	   result.setId(transform(patient.getIdentifierFirstRep()));
	   if (patient.hasName()) result.setName(transform(patient.getNameFirstRep()));	   
	   return result;
	}
	
	/**
	 * FHIR RelatedPerson -> XDS Person
	 * @param related
	 * @return
	 */
	public Person transform(RelatedPerson related) {
		if (related == null) return null;
	   Person result = new Person();
	   result.setId(transform(related.getIdentifierFirstRep()));
	   if (related.hasName()) result.setName(transform(related.getNameFirstRep()));	   
	   return result;
	}	
	
	/**
	 * FHIR ContactPoint -> XDS Telecom
	 * @param contactPoint
	 * @return
	 */
	public Telecom transform(ContactPoint contactPoint) {
		if (contactPoint == null) return null;
    	Telecom result = new Telecom();
    	
    	if (contactPoint.getSystem().equals(ContactPointSystem.EMAIL)) {
    		result.setEmail(contactPoint.getValue());   
    		result.setUse("NET");
    		result.setType("Internet");
    	} else if (contactPoint.getSystem().equals(ContactPointSystem.PHONE)) {
    		result.setUnformattedPhoneNumber(contactPoint.getValue());    
    		if (contactPoint.getUse().equals(ContactPoint.ContactPointUse.WORK)) result.setUse("WPN");
    		else result.setUse("PRN");
    		result.setType("PH");
    	}
    	    	
    	return result;
    }
	
	/**
	 * FHIR Organization -> XDS Organization
	 * @param org
	 * @return
	 */
	public org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization transform(Organization org) {
		org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization result = new org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization();
    	result.setOrganizationName(org.getName());
    	Identifier identifier = org.getIdentifierFirstRep();
    	if (identifier != null) {
    		result.setIdNumber(identifier.getValue());
    		result.setAssigningAuthority(new AssigningAuthority(noPrefix(identifier.getSystem())));
    	}    	
    	return result;
    }
	
	/**
	 * FHIR Reference to Author -> XDS Author
	 * @param author
	 * @param contained
	 * @return
	 */
	public Author transformAuthor(Reference author, List<Resource> contained) {
		if (author == null || author.getReference() == null) return null;
		Resource authorObj = findResource(author, contained);
		
		if (authorObj instanceof Practitioner) {
			Practitioner practitioner = (Practitioner) authorObj;
			Author result = new Author();
			result.setAuthorPerson(transform((Practitioner) authorObj));
			for (ContactPoint contactPoint : practitioner.getTelecom()) result.getAuthorTelecom().add(transform(contactPoint));
			return result;
		} else if (authorObj instanceof PractitionerRole) { 
			Author result = new Author();
			PractitionerRole role = (PractitionerRole) authorObj;
			Practitioner practitioner = (Practitioner) findResource(role.getPractitioner(), contained);
			if (practitioner != null) result.setAuthorPerson(transform(practitioner));
		    Organization org = (Organization) findResource(role.getOrganization(), contained);
		    if (org != null) result.getAuthorInstitution().add(transform(org));
		    for (CodeableConcept code : role.getCode()) result.getAuthorRole().add(transformToIdentifiable(code));
		    for (CodeableConcept speciality : role.getSpecialty()) result.getAuthorSpecialty().add(transformToIdentifiable(speciality));
		    for (ContactPoint contactPoint : role.getTelecom()) result.getAuthorTelecom().add(transform(contactPoint));
		    return result;
		}
		
    	return null;
    }
	
	
}
