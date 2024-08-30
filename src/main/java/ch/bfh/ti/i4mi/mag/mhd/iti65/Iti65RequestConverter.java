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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.activation.DataHandler;

import ch.bfh.ti.i4mi.mag.MagConstants;
import lombok.Setter;
import org.apache.camel.Body;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Annotation;
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
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.IdentifierUse;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.openehealth.ipf.commons.ihe.fhir.support.FhirUtils;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.CXiAssigningAuthority;
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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import lombok.extern.slf4j.Slf4j;

/**
 * ITI-65 to ITI-41 request converter
 * @author alexander kreutz
 *
 */
@Slf4j
public class Iti65RequestConverter extends BaseRequestConverter {

	private SchemeMapper schemeMapper;

	@Autowired
	@Setter
	private Config config;

	@Autowired
	public void setSchemeMapper(SchemeMapper schemeMapper) {
		this.schemeMapper = schemeMapper;
	}

	@Autowired
	@Setter
	private PatientReferenceCreator patientRefCreator;

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

		ListResource manifestNeu = null;

		for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
            Resource resource = requestEntry.getResource();
            /*if (resource instanceof DocumentManifest) {
            	manifest = (DocumentManifest) resource;            	
            } else*/ if (resource instanceof DocumentReference) {
            	resources.put(requestEntry.getFullUrl(), resource);

            } else if (resource instanceof ListResource) {
            	manifestNeu = (ListResource) resource;
            	//resources.put(requestEntry.getFullUrl(), resource);
            } else if (resource instanceof Binary) {
            	resources.put(requestEntry.getFullUrl(), resource);
            } else {
                throw new IllegalArgumentException(resource + " is not allowed here");
            }
        }
					
	    /*if (manifest != null) {
		  processDocumentManifest(manifest, submissionSet);
	    } else {*/
	      processDocumentManifest(manifestNeu, submissionSet);
	    //}

		// set limited metadata
		for (CanonicalType profile : requestBundle.getMeta().getProfile()) {
			if ("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Comprehensive_DocumentBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(false);
			} else if ("http://ihe.net/fhir/StructureDefinition/IHE_MHD_Provide_Minimal_DocumentBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(true);
			} else if ("http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(false);
			} else if ("http://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.ProvideBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(true);
			} else if ("https://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Comprehensive.ProvideBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(false);
			} else if ("https://profiles.ihe.net/ITI/MHD/StructureDefinition/IHE.MHD.Minimal.ProvideBundle".equals(profile.getValue())) {
				submissionSet.setLimitedMetadata(true);
			}
		}

		// process all resources referenced in DocumentManifest.content
		for (ListEntryComponent listEntry : manifestNeu.getEntry()) {
			Reference content = listEntry.getItem();
			String refTarget = content.getReference();
			Resource resource = resources.get(refTarget);
			if (resource instanceof DocumentReference) {
				DocumentReference documentReference = (DocumentReference) resource;
				Document doc = new Document();
        		DocumentEntry entry = new DocumentEntry();
				entry.setExtraMetadata(new HashMap<>());
                processDocumentReference(documentReference, entry);
                doc.setDocumentEntry(entry);

				String uniqueRepositoryId = config.getRepositoryUniqueId();
				if (uniqueRepositoryId != null && !uniqueRepositoryId.isBlank()) {
					entry.setRepositoryUniqueId(uniqueRepositoryId);
				}

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
                	byte[] decoded = attachment.getData();
                    entry.setSize((long) decoded.length);
                    entry.setHash(SHAsum(decoded));
                } else if (attachment.hasUrl()) {
                    String contentURL = attachment.getUrl();
	                Resource binaryContent = resources.get(contentURL);
	                if (binaryContent instanceof Binary) {
	                	String contentType = attachment.getContentType();
	                	Binary binary = (Binary) binaryContent;
	                	if (binary.hasContentType() && !binary.getContentType().equals(contentType)) throw new InvalidRequestException("ContentType in Binary and in DocumentReference must match");
	                	doc.setDataHandler(new DataHandler(new ByteArrayDataSource(binary.getData(),contentType)));
	                	byte[] decoded = binary.getData();
	                    entry.setSize((long) decoded.length);
	                    entry.setHash(SHAsum(decoded));
	                	Identifier masterIdentifier = documentReference.getMasterIdentifier();
	                    binary.setUserData("masterIdentifier", noPrefix(masterIdentifier.getValue()));
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
		    // FIX FOR CARA
		    return new LocalizedString(string,"en","UTF-8");
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
		String displayName = StringUtils.isEmpty(coding.getDisplay()) ? coding.getCode() : coding.getDisplay();
		return new Code(coding.getCode(), localizedString(displayName), schemeMapper.getScheme(coding.getSystem()));
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
	 * removes separator in date and time, except the timezone differentiator
	 * @param fhirDate  e.g. 2015-02-07T13:28:17-05:00
	 * @return  2015020T132817-0500
	 */
	public Timestamp removeSeparatorsExceptForTimezone(String fhirDate) {
	  if (fhirDate==null) {
	    return null;
	  }
	  int timePos = fhirDate.indexOf("T");
	  String dateString = (timePos>=0 ? fhirDate.substring(0, timePos) : fhirDate);
	  dateString = dateString.replace("-", "");
	  if (timePos>=0) {
	    dateString += fhirDate.substring(timePos+1).replace(":", "");
	  }
    return Timestamp.fromHL7(dateString);
	}

	/**
	 * FHIR DateType -> XDS Timestamp
	 * @param date
	 * @return
	 */
	public  Timestamp timestampFromDate(DateType date) {
    	if (date == null) return null;
    	return removeSeparatorsExceptForTimezone(date.asStringValue());
    }

	/**
	 * FHIR DateTimeType -> XDS Timestamp
	 * @param date
	 * @return
	 */
	public  Timestamp timestampFromDate(DateTimeType date) {
    if (date == null) return null;
    return removeSeparatorsExceptForTimezone(date.asStringValue());
    }

	/**
	 * FHIR InstantType -> XDS Timestamp
	 * @param date
	 * @return
	 */
	public  Timestamp timestampFromDate(InstantType date) {
    if (date == null) return null;
    return removeSeparatorsExceptForTimezone(date.asStringValue());
    }

	/**
	 * FHIR Coding -> XDS Code
	 * @param coding
	 * @return
	 */
	public  Code transform(Coding coding) {
		if (coding==null) return null;
		String displayName = StringUtils.isEmpty(coding.getDisplay()) ? coding.getCode() : coding.getDisplay();
		return new Code(coding.getCode(), localizedString(displayName), schemeMapper.getScheme(coding.getSystem()));
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
		targetAddress.setCountyParishCode(address.getDistrict());
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
	static public String noPrefix(String system) {
		if (system == null) return null;
		if (system.startsWith("urn:oid:")) {
            system = system.substring(8);
        }
		if (system.startsWith("urn:uuid:")) {
            system = system.substring(9);
        }
		return system;
	}

	public String noBaseUrl(String in) {
		if (in == null) return null;
		return new IdType(in).getIdPart();
	}

	/**
	 * FHIR Identifier -> XDS Identifiable
	 * @param identifier
	 * @return
	 */
	public Identifiable transform(Identifier identifier) {
		String system = noPrefix(identifier.getSystem());
		if (identifier.hasSystem()) {
			return new Identifiable(identifier.getValue(), new AssigningAuthority(system));
		}
		return new Identifiable(identifier.getValue());
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
			if (targetRef.startsWith("#")) {
//				targetRef = targetRef.substring(1);  note: resource.getId() return the id wit the # prefix for the contained resources
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
			}

			Identifiable result = patientRefCreator.resolvePatientReference(reference.getReference());
			if (result != null) 
				return result;

			MultiValueMap<String, String> vals = UriComponentsBuilder.fromUriString(targetRef).build().getQueryParams();
			if (vals.containsKey("identifier")) {
				String ids = vals.getFirst("identifier");
				if (ids == null) return null;
				String[] identifier = ids.split("\\|");
				if (identifier.length == 2) {
					return new Identifiable(identifier[1], new AssigningAuthority(noPrefix(identifier[0])));
				}
			}
			// try to resolved it by fetching the url
			String ref = reference.getReference();
			if (ref!=null && ref.startsWith("http") && ref.contains("/Patient/")) {
				String fhirBase = ref.substring(0, ref.indexOf("/Patient/"));

				if (!fhirBase.equals(config.getUriExternalPatientEndpoint())) {
					log.error("Patient url must be in the form "+config.getUriExternalPatientEndpoint()+"/Patient/... but was "+ref);
					throw FhirUtils.invalidRequest(
										OperationOutcome.IssueSeverity.ERROR,
										OperationOutcome.IssueType.INVALID,
										null, null,
										"Patient url must be in the form "+config.getUriExternalPatientEndpoint()+"/Patient/... but was "+ref
								);
				}

				String patientId = ref.substring(ref.indexOf("/Patient/")+9);
				GenericClient client = new GenericClient(FhirContext.forR4Cached(), null, fhirBase, null);
				client.setDontValidateConformance(true);
				Patient patient = client.read(Patient.class, patientId);
				if (patient!=null && patient.hasIdentifier()) {
					String oidString = "urn:oid:"+config.getOidMpiPid();
					for (Identifier identifier : patient.getIdentifier()) {
						if (oidString.equals(identifier.getSystem())) {
							return transform(identifier);
						}
					}
				}

				throw FhirUtils.invalidRequest(
									OperationOutcome.IssueSeverity.ERROR,
									OperationOutcome.IssueType.INVALID,
									null, null,
									"Patient not found or or no identifier for affinity domain  "+ref+" "+config.getOidMpiPid()
							);

			}
		} else if (reference.hasIdentifier()) {
	        return transform(reference.getIdentifier());
		}
		throw new InvalidRequestException("Cannot resolve reference to "+(reference.getReference()!=null ? reference.getReference().toString(): ""));
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
		String result = noBaseUrl(noPrefix(ref.getReference()));
		if (!result.startsWith("urn:")) result = "urn:uuid:" + result;
		return result;
	}


	/**
	 * ITI-65: process ListResource resource from Bundle
	 * @param manifest
	 * @param submissionSet
	 */
	private  void processDocumentManifest(ListResource manifest, SubmissionSet submissionSet) {

		for (Identifier id : manifest.getIdentifier()) {
			if (id.getUse() == null ||  id.getUse().equals(Identifier.IdentifierUse.OFFICIAL)) {

			} else if (id.getUse().equals(Identifier.IdentifierUse.USUAL)) {
				String uniqueId = noPrefix(id.getValue());
				submissionSet.setUniqueId(uniqueId);
			}
		}
		if (submissionSet.getUniqueId() == null) throw FhirUtils.invalidRequest(
				OperationOutcome.IssueSeverity.ERROR,
				OperationOutcome.IssueType.INVALID,
				null, null,
				"List.identifier with use usual missing"
		);

		submissionSet.assignEntryUuid();
		manifest.setId(submissionSet.getEntryUuid());
		
		Extension designationType = getExtensionByUrl(manifest, "https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType");
		
		if (designationType != null && designationType.getValue() instanceof CodeableConcept) {
			submissionSet.setContentTypeCode(transformCodeableConcept((CodeableConcept) designationType.getValue()));
		}

		DateTimeType created = manifest.getDateElement();

		if (created == null || !created.hasValue()) throw FhirUtils.invalidRequest(
				OperationOutcome.IssueSeverity.ERROR,
				OperationOutcome.IssueType.INVALID,
				null, null,
				"List.date missing"
		);
		submissionSet.setSubmissionTime(timestampFromDate(created));

		//  subject	SubmissionSet.patientId
		Reference ref = manifest.getSubject();
		submissionSet.setPatientId(transformReferenceToIdentifiable(ref, manifest));

		// Author
        Extension authorRoleExt = manifest.getExtensionByUrl(MagConstants.FhirExtensionUrls.CH_AUTHOR_ROLE);
		if (manifest.hasSource() || (authorRoleExt!=null)) {
		    Identifiable identifiable = null;
			Reference author = manifest.getSource();
            if (authorRoleExt!=null) {
                Coding coding = authorRoleExt.castToCoding(authorRoleExt.getValue());
                if (coding !=null) {
                    identifiable = new Identifiable(coding.getCode(), new AssigningAuthority(noPrefix(coding.getSystem())));
                }
            }
			submissionSet.setAuthor(transformAuthor(author, manifest.getContained(), identifiable));
		}
		 // recipient	SubmissionSet.intendedRecipient		
		
		List<Extension> recipients = manifest.getExtensionsByUrl("https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-intendedRecipient");
		if (recipients.isEmpty()) recipients = manifest.getExtensionsByUrl("http://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-intendedRecipient");
		
		for (Extension recipientExt : recipients) {
			Reference recipientRef = (Reference) recipientExt.getValue();
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
		
		Extension source = getExtensionByUrl(manifest, "https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId");
		
		if (source != null && source.getValue() instanceof Identifier) {
		  submissionSet.setSourceId(noPrefix(((Identifier) source.getValue()).getValue()));
		}

		String title = manifest.getTitle();
		if (title!=null) submissionSet.setTitle(localizedString(title));

		Annotation note = manifest.getNoteFirstRep();
		if (note != null && note.hasText()) {
		  submissionSet.setComments(localizedString(note.getText()));
		}

	}

	/**
	 * ITI-65: process DocumentReference resource from Bundle
	 * @param reference
	 * @param entry
	 */
	public  void processDocumentReference(DocumentReference reference, DocumentEntry entry) {

		//if (reference.getIdElement()!=null) {
		//	entry.setEntryUuid(reference.getIdElement().getIdPart());
		//} else {
		entry.assignEntryUuid();
		//}

        reference.getIdentifier().stream()
                .filter(identifier -> identifier.hasType() && identifier.getType().hasCoding(MagConstants.FhirCodingSystemIds.MHD_DOCUMENT_ID_TYPE, "logicalID"))
                .findAny()
                .map(Identifier::getValue)
				.ifPresent(entry::setLogicalUuid);

		String logicalId = (entry.getLogicalUuid() != null) ? entry.getLogicalUuid() : entry.getEntryUuid();
		reference.setId(logicalId);

		Identifier masterIdentifier = reference.getMasterIdentifier();
        if (masterIdentifier == null || !masterIdentifier.hasValue() || masterIdentifier.getValue() == null || masterIdentifier.getValue().isEmpty()) throw FhirUtils.invalidRequest(
				OperationOutcome.IssueSeverity.ERROR,
				OperationOutcome.IssueType.INVALID,
				null, null,
				"DocumentReference.masterIdentifier missing"
		);
        entry.setUniqueId(noPrefix(masterIdentifier.getValue()));

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
		   entry.getAuthors().add(transformAuthor(authorRef, reference.getContained(),null));
		}

        // legalAuthenticator -> authenticator Note 1

		if (reference.hasAuthenticator()) {
			 Reference authenticatorRef = reference.getAuthenticator();
			 Resource authenticator = findResource(authenticatorRef, reference.getContained());
			 if (authenticator instanceof Practitioner) {
				 entry.setLegalAuthenticator(transform((Practitioner) authenticator));
			 } else if (authenticator instanceof PractitionerRole) {
				 Practitioner practitioner = (Practitioner) findResource(((PractitionerRole) authenticator).getPractitioner(), reference.getContained());
				 if (practitioner != null) entry.setLegalAuthenticator(transform(practitioner));
			 } else throw new InvalidRequestException("No authenticator of type Organization supported.");
		}

        // comments -> description string [0..1]
		String comments = reference.getDescription();
		if (comments != null) entry.setComments(localizedString(comments));

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
        if (hash != null) entry.setHash(Hex.encodeHexString(hash));

        // title -> content.attachment.title string [0..1]
        String title = attachment.getTitle();
        if (title!=null) entry.setTitle(localizedString(title));

        // creationTime -> content.attachment.creation dateTime [0..1]
        if (attachment.hasCreation()) {
        	if (entry.getCreationTime() == null) entry.setCreationTime(timestampFromDate(attachment.getCreationElement()));
        	else if (!timestampFromDate(attachment.getCreationElement()).equals(entry.getCreationTime())) throw new InvalidRequestException("DocumentReference.date does not match attachment.creation element");
        }

        // formatCode -> content.format Coding [0..1]
        Coding coding = content.getFormat();
        entry.setFormatCode(transform(coding));

        DocumentReferenceContextComponent context = reference.getContext();

        // referenceIdList -> context.encounter Reference(Encounter) [0..*] When
        // referenceIdList contains an encounter, and a FHIR Encounter is available, it
        // may be referenced.
        // We do not support this
        //
        // Instead: referenceIdList -> related.identifier
        for (Reference ref : context.getRelated()) {
        	Identifiable refId = transformReferenceToIdentifiable(ref, reference);
        	if (refId != null) {
        		ReferenceId referenceId = new ReferenceId();
        		referenceId.setAssigningAuthority(new CXiAssigningAuthority(null, refId.getAssigningAuthority().getUniversalId(), refId.getAssigningAuthority().getUniversalIdType()));
          	    referenceId.setId(refId.getId());
        		entry.getReferenceIdList().add(referenceId);
        	}
        }


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

        Extension originalRole = reference.getExtensionByUrl(MagConstants.FhirExtensionUrls.CH_AUTHOR_ROLE);
        if (originalRole != null) {
        	if (originalRole.getValue() instanceof Coding) {
        		Coding value = (Coding) originalRole.getValue();
        		String system = noPrefix(value.getSystem());
        		String code = value.getCode();
        		entry.getExtraMetadata().put(MagConstants.XdsExtraMetadataSlotNames.CH_ORIGINAL_PROVIDER_ROLE, Collections.singletonList(code+"^^^&"+system+"&ISO"));
        	}
        }

        Extension deletionStatus = reference.getExtensionByUrl(MagConstants.FhirExtensionUrls.CH_DELETION_STATUS);
        if (deletionStatus != null) {
        	if (deletionStatus.getValue() instanceof Coding) {
        		Coding value = (Coding) deletionStatus.getValue();
        		String code = value.getCode();
				entry.getExtraMetadata().put(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS, Collections.singletonList(code));
        	}
        }

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
	   if (practitioner.hasIdentifier()) {
		   result.setId(transform(practitioner.getIdentifierFirstRep()));
	   }
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
	   if (patient.hasIdentifier()) {
		   result.setId(transform(patient.getIdentifierFirstRep()));
		}
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
	   if (related.hasIdentifier()) {
		   result.setId(transform(related.getIdentifierFirstRep()));
	   }
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

    	if (contactPoint.getSystem().equals(ContactPointSystem.EMAIL) || contactPoint.getSystem().equals(ContactPointSystem.URL)) {
    		result.setEmail(contactPoint.getValue());
    		result.setUse("NET");
    		result.setType("Internet");
    	} else {
    		result.setUnformattedPhoneNumber(contactPoint.getValue());
    		if (contactPoint.hasSystem())
    		switch (contactPoint.getSystem()) {
	    		case SMS:
	    		case PHONE:result.setType("PH");break;
	    		case FAX:result.setType("FX");break;
	    		case PAGER:result.setType("BP");break;
    		}

    		if (contactPoint.hasUse())
    		switch (contactPoint.getUse()) {
	    		case HOME: result.setUse("PRN");break;
	    		case WORK: result.setUse("WPN");break;
	    		case MOBILE: result.setType("CP");break;
    		}

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
	public Author transformAuthor(Reference author, List<Resource> contained, Identifiable authorRole) {
		if (author == null || author.getReference() == null) {
		    if (authorRole!=null) {
	            Author result = new Author();
	            Person person = new Person();
	            // CARA PMP
	            // At least an authorPerson, authorTelecommunication, or authorInstitution sub-attribute must be present
	            // Either authorPerson, authorInstitution or authorTelecom shall be specified in the SubmissionSet [IHE ITI Technical Framework Volume 3 (4.2.3.1.4)].
	            person.setName(transform(new HumanName().setFamily("---")));
	            result.setAuthorPerson(person);
	            result.getAuthorRole().add(authorRole);
	            return result;
		    }
		    return null;
		}
		Resource authorObj = findResource(author, contained);
		if (authorObj instanceof Practitioner) {
			Practitioner practitioner = (Practitioner) authorObj;
			Author result = new Author();
			result.setAuthorPerson(transform((Practitioner) authorObj));
			for (ContactPoint contactPoint : practitioner.getTelecom()) result.getAuthorTelecom().add(transform(contactPoint));
			if (authorRole==null) {
			    authorRole = new Identifiable("HCP", new AssigningAuthority("2.16.756.5.30.1.127.3.10.6"));
			}
			result.getAuthorRole().add(authorRole);
			return result;
		} else if (authorObj instanceof Patient) {
			Patient patient = (Patient) authorObj;
			Author result = new Author();
			result.setAuthorPerson(transform(patient));
			for (ContactPoint contactPoint : patient.getTelecom()) result.getAuthorTelecom().add(transform(contactPoint));
            if (authorRole==null) {
                authorRole = new Identifiable("PAT", new AssigningAuthority("2.16.756.5.30.1.127.3.10.6"));
            }
            result.getAuthorRole().add(authorRole);
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
		} else throw new InvalidRequestException("Author role not supported.");

    	//return null;
    }

	public String SHAsum(byte[] convertme) {
		try {
	    MessageDigest md = MessageDigest.getInstance("SHA-1");
	    return Hex.encodeHexString(md.digest(convertme));
		} catch (NoSuchAlgorithmException e) { return ""; }
	}

	/*private  String byteArray2Hex(final byte[] hash) {
	    Formatter formatter = new Formatter();
	    for (byte b : hash) {
	        formatter.format("%02x", b);
	    }
	    return formatter.toString();
	}*/
	/*
	private String base64ToHex(String input) {
		byte[] decoded = Base64.getDecoder().decode(input);
		return Hex.encodeHexString(decoded);
	}
	*/


}
