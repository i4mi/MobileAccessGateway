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
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContextComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceRelatesToComponent;
import org.hl7.fhir.r4.model.DocumentReference.DocumentRelationshipType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Document;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Name;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.PatientInfo;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
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
 * @author alexander
 *
 */
@Slf4j
public class Iti65RequestConverter {

	private SchemeMapper schemeMapper;
	
	@Autowired
	public void setSchemeMapper(SchemeMapper schemeMapper) {
		this.schemeMapper = schemeMapper;
	}
	
	public ProvideAndRegisterDocumentSet convert(@Body Bundle requestBundle) {
		
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
		
		for (CanonicalType profile : requestBundle.getMeta().getProfile()) {
			if ("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(false);
			} else if ("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(true);
			}
		}
		
		for (Reference content : manifest.getContent()) {
			String refTarget = content.getReference();
			Resource resource = resources.get(refTarget);
			if (resource instanceof DocumentReference) {
				DocumentReference documentReference = (DocumentReference) resource;
				Document doc = new Document();            	
        		DocumentEntry entry = new DocumentEntry();        		        		        		
                processDocumentReference(documentReference, entry);
                doc.setDocumentEntry(entry);
                
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
	
	public LocalizedString localizedString(String string) {
		if (string==null) return null;
		return new LocalizedString(string);
	}
	
	public  Code transformCodeableConcept(CodeableConcept cc) {
		if (cc == null) return null;
		if (!cc.hasCoding()) return null;
		Coding coding = cc.getCodingFirstRep();
		return new Code(coding.getCode(), localizedString(coding.getDisplay()), schemeMapper.getScheme(coding.getSystem()));
	}
	
	public  void transformCodeableConcepts(List<CodeableConcept> ccs, List<Code> target) {
		if (ccs == null || ccs.isEmpty()) return;
		for (CodeableConcept cc : ccs) {
			Code code = transformCodeableConcept(cc); 
			if (code!=null) target.add(code);
		}
	}
	
	public  Timestamp timestampFromDate(DateType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("[T\\-:]","");    	
    	return Timestamp.fromHL7(dateString);
    }
	
	public  Timestamp timestampFromDate(DateTimeType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("[T\\-:]","");    	
    	return Timestamp.fromHL7(dateString);
    }
	
	public  Timestamp timestampFromDate(InstantType date) {
    	if (date == null) return null; 
    	String dateString = date.asStringValue();
    	if (dateString==null) return null;
    	dateString = dateString.replaceAll("[T\\-:]","");    	
    	return Timestamp.fromHL7(dateString);
    }
	
	public  Code transform(Coding coding) {
		if (coding==null) return null;
		return new Code(coding.getCode(), localizedString(coding.getDisplay()), schemeMapper.getScheme(coding.getSystem()));
	}
	
	public  Code transform(CodeableConcept cc) {
		if (cc==null) return null;
		Coding coding = cc.getCodingFirstRep();
		return transform(coding);
	}
	
	public  Code transform(List<CodeableConcept> ccs) {
		if (ccs==null || ccs.isEmpty()) return null;
		return transform(ccs.get(0));
	}
	
	public String noPrefix(String system) {
		if (system == null) return null;
		if (system.startsWith("urn:oid:")) {
            system = system.substring(8);
        }
		return system;
	}
	
	public Identifiable transform(Identifier identifier) {
		String system = noPrefix(identifier.getSystem());			    	
		return new Identifiable(identifier.getValue(), new AssigningAuthority(system));
	}
	
	public  Identifiable transformReferenceToIdentifiable(Reference reference, DomainResource container) {
		if (reference.hasReference()) {
			String targetRef = reference.getReference();		
			List<Resource> resources = container.getContained();		
			for (Resource resource : resources) {			
				if (targetRef.equals(resource.getId())) {
					return transform(((Patient) resource).getIdentifierFirstRep());					
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
				switch(gender) {
				case MALE: patientInfo.setGender("M");break;
				case FEMALE: patientInfo.setGender("F");break;
				case OTHER: patientInfo.setGender("A");break;
				default: patientInfo.setGender("U");break;
				}
				
				for (HumanName name : patient.getName()) {
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
					patientInfo.getNames().add(targetName);	
				}
				
				for (Address address : patient.getAddress()) {
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
				    
				    patientInfo.getAddresses().add(targetAddress);
				}
				
				for (Identifier id : patient.getIdentifier()) {
				  patientInfo.getIds().add(transform(id));
				}
				
				return patientInfo;
			}
		}
		return null;
	}
	
	private String transformUriFromReference(Reference ref) {
		if (ref.hasIdentifier()) {
			return ref.getIdentifier().getValue();
		}
		return noPrefix(ref.getReference());
	}
	
	private  void processDocumentManifest(DocumentManifest manifest, SubmissionSet submissionSet) {
		// masterIdentifier	SubmissionSet.uniqueId
		Identifier masterIdentifier = manifest.getMasterIdentifier();
		submissionSet.setUniqueId(noPrefix(masterIdentifier.getValue()));
		   
		
		submissionSet.assignEntryUuid();
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
		String source = noPrefix(manifest.getSource());		
		submissionSet.setSourceId(source);
		  
		String description = manifest.getDescription();		
		if (description!=null) submissionSet.setTitle(localizedString(description));		  
		
	}
	
	private  void processDocumentReference(DocumentReference reference, DocumentEntry entry) {
		 // FIXME String uuid = UUID.randomUUID().toString();
		//entry.setEntryUuid(reference.getId());
        entry.assignEntryUuid();
        
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
		entry.setCreationTime(timestampFromDate(reference.getDateElement()));

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
		if (content==null) throw new NullPointerException(); // TODO throw error
		Attachment attachment = content.getAttachment();
		if (attachment==null) throw new NullPointerException(); // TODO throw error
		entry.setMimeType(attachment.getContentType());
		       
        // languageCode -> content.attachment.language code [0..1]
		entry.setLanguageCode(attachment.getLanguage());

        // size -> content.attachment.size integer [0..1] The size is calculated
        entry.setSize((long) attachment.getSize());
        
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
        //List<ReferenceId> refIds = documentEntry.getReferenceIdList();
        //if (refIds!=null) {
        //	for (ReferenceId refId : refIds) {
        //		if (ReferenceId.ID_TYPE_ENCOUNTER_ID.equals(refId.getIdTypeCode())) {
        //			context.addEncounter(transform(refId));
        //		}
        //	}
        //}
        
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
	
	
}
