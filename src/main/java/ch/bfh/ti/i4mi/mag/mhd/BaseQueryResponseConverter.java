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
package ch.bfh.ti.i4mi.mag.mhd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.codesystems.ContactPointUse;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Address;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Author;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.CXiAssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.LocalizedString;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Name;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Person;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.ReferenceId;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Telecom;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp.Precision;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;

/**
 * base query response converter XDS to MHD
 * @author alexander kreutz
 *
 */
public abstract class BaseQueryResponseConverter extends BaseResponseConverter implements ToFhirTranslator<QueryResponse> {

	private SchemeMapper schemeMapper;
	private PatientReferenceCreator patientReferenceCreator;
	
	protected final Config config;

    public BaseQueryResponseConverter(final Config config) {
        this.config = config;
        schemeMapper = config.getSchemeMapper();
        patientReferenceCreator = config.getPatientReferenceCreator();
    }
	
    /**
     * XDS scheme name -> FHIR system
     * @param schemeName
     * @return
     */
    public String getSystem(String schemeName) {
    	return schemeMapper.getSystem(schemeName);        
    }
    
    public String asUuid(String urn) {
    	if (urn == null) return null;
    	if (urn.startsWith("urn:uuid:")) return urn;
    	return "urn:uuid:"+urn;
    }
    
    public String noUuidPrefix(String urn) {
    	if (urn == null) return null;
    	if (urn.startsWith("urn:uuid:")) return urn.substring("urn:uuid:".length());
    	return urn;
    }
    
    public String noOidPrefix(String urn) {
    	if (urn == null) return null;
    	if (urn.startsWith("urn:oid:")) return urn.substring("urn:oid:".length());
    	return urn;
    }

    /**
     * XDS code -> FHIR CodeableConcept
     * @param code
     * @return
     */
    public CodeableConcept transform(Code code) {
        return new CodeableConcept().addCoding(new Coding().setCode(code.getCode())
                .setSystem(getSystem(code.getSchemeName())).setDisplay(code.getDisplayName()==null ? "" : code.getDisplayName().getValue()));
    }

    /**
     * XDS code list -> FHIR CodeableConcept
     * @param codes
     * @return
     */
    public CodeableConcept transform(List<Code> codes) {
        CodeableConcept cc = new CodeableConcept();
        if (codes!=null) {
            for(Code code: codes) {
                cc.addCoding(new Coding().setCode(code.getCode()).setSystem(getSystem(code.getSchemeName())).setDisplay(code.getDisplayName()==null ? "" : code.getDisplayName().getValue()));
            }
        }
        return cc; 
    }
    
    /**
     * XDS code list -> FHIR CodeableConcept list
     * @param codes
     * @return
     */
    public List<CodeableConcept> transformMultiple(List<Code> codes) {
        List<CodeableConcept> ccList = new ArrayList<CodeableConcept>();
        if (codes!=null) {
            for(Code code: codes) {
            	ccList.add(transform(code));               
            }
        }
        return ccList; 
    }
    
    /**
     * XDS timestamp -> FHIR DateTime
     * @param timestamp
     * @return
     */
    public DateTimeType transform(Timestamp timestamp) {
    	if (timestamp == null) return null;
    	Date date = Date.from(timestamp.getDateTime().toInstant());
    	Precision precision = timestamp.getPrecision();
    	TemporalPrecisionEnum fhirPrecision;
    	switch(precision) {
    	case YEAR: fhirPrecision = TemporalPrecisionEnum.YEAR;break;
    	case DAY: fhirPrecision = TemporalPrecisionEnum.DAY;break;
    	// There is no mapping for HOUR; MINUTE is not accepted
    	case HOUR: fhirPrecision = TemporalPrecisionEnum.SECOND;break;
    	case MINUTE: fhirPrecision = TemporalPrecisionEnum.SECOND;break;
    	case SECOND: fhirPrecision = TemporalPrecisionEnum.SECOND;break;
    	default: fhirPrecision = TemporalPrecisionEnum.MILLI;break;
    	}
    	return new DateTimeType(date, fhirPrecision);
    }
    
    /**
     * XDS Timestamp -> FHIR Date
     * @param timestamp
     * @return
     */
    public DateType transformToDate(Timestamp timestamp) {
    	if (timestamp == null) return null;
    	Date date = Date.from(timestamp.getDateTime().toInstant());
    	Precision precision = timestamp.getPrecision();
    	TemporalPrecisionEnum fhirPrecision;
    	switch(precision) {
    	case YEAR: fhirPrecision = TemporalPrecisionEnum.YEAR;break;
    	case DAY: fhirPrecision = TemporalPrecisionEnum.DAY;break;
    	// There is no mapping for HOUR
    	//case HOUR: fhirPrecision = TemporalPrecisionEnum.MINUTE;break;
    	//case MINUTE: fhirPrecision = TemporalPrecisionEnum.MINUTE;break;
    	//case SECOND: fhirPrecision = TemporalPrecisionEnum.SECOND;break;
    	default: fhirPrecision = TemporalPrecisionEnum.DAY;break;
    	}
    	return new DateType(date, fhirPrecision);
    }
    
    /**
     * XDS ReferenceId -> FHIR Reference
     * @param ref
     * @return
     */
    public Reference transform(ReferenceId ref) {
    	String id = ref.getId();
    	CXiAssigningAuthority authority = ref.getAssigningAuthority();
    	// TODO handle authority not given
    	if (authority != null) {
    		return new Reference().setIdentifier(new Identifier().setValue(id).setSystem(getSystem(authority.getUniversalId())));
    	} else {
    		return new Reference().setIdentifier(new Identifier().setValue(id));
    	}
     }
    
    /**
     * XDS Person -> FHIR Practitioner
     * @param person
     * @return
     */
    public Practitioner transformPractitioner(Person person) {
    	if (person==null) return null;
    	Practitioner practitioner = new Practitioner();
    	Name name = person.getName();
    	if (name != null) {
    	  practitioner.addName(transform(name));
    	}
    	if (person.getId()!=null) practitioner.addIdentifier(transformToIdentifier(person.getId()));
    	    	
    	return practitioner;
    }
    
    /**
     * XDS Person -> FHIR Patient
     * @param person
     * @return
     */
    public Patient transformPatient(Person person) {
    	if (person==null) return null;
    	Patient patient = new Patient();
    	Name name = person.getName();
    	if (name != null) {
    		patient.addName(transform(name));
    	}
    	if (person.getId()!=null) patient.addIdentifier(transformToIdentifier(person.getId()));
    	    	
    	return patient;
    }
    
    /**
     * XDS Organization -> FHIR Organization
     * @param org
     * @return
     */
    public Organization transform(org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization org) {
    	if (org == null) return null;
    	Organization result = new Organization();
    	result.setName(org.getOrganizationName());
    	String id = org.getIdNumber();
    	// TODO handle system not given
    	
    	if (org.getAssigningAuthority()!=null) {
    	   String system = org.getAssigningAuthority().getUniversalId();
    	   result.addIdentifier().setSystem("urn:oid:"+system).setValue(id);
    	} else result.addIdentifier().setValue(id);
    	return result;
    }
    
    /**
     * XDS Telecom -> FHIR ContactPoint
     * @param telecom
     * @return
     */
    public ContactPoint transform(Telecom telecom) {
    	ContactPoint result = new ContactPoint();
    	
    	String type = telecom.getType();
    	String use = telecom.getUse();
    	    	
    	if ("NET".equals(use) || "X.400".equals(type)) { 
    	  result.setSystem(ContactPointSystem.EMAIL);    	
    	  result.setValue(telecom.getEmail());    	  
    	} else {    
    	  String phone = telecom.getUnformattedPhoneNumber();
    	  result.setValue(phone);
    	  if ("WPN".equals(use)) result.setUse(ContactPoint.ContactPointUse.WORK);
    	  else if ("PRN".equals(use)) result.setUse(ContactPoint.ContactPointUse.HOME);
    	  
    	  if ("FX".equals(type)) result.setSystem(ContactPointSystem.FAX);
    	  else if ("BP".equals(type)) result.setSystem(ContactPointSystem.PAGER);
    	  else if ("CP".equals(type)) { result.setSystem(ContactPointSystem.PHONE);result.setUse(ContactPoint.ContactPointUse.MOBILE); }
    	  else result.setSystem(ContactPointSystem.PHONE);

    	}
    	    	
    	return result;
    }
    
    /**
     * XDS Name -> FHIR HumanName
     * @param name
     * @return
     */
    public HumanName transform(Name name) {
    	HumanName result = new HumanName();
    	if (name.getPrefix() != null) result.addPrefix(name.getPrefix());
    	if (name.getSuffix() != null) result.addSuffix(name.getSuffix());    	    
    	result.setFamily(name.getFamilyName()).addGiven(name.getGivenName());
    	String more = name.getSecondAndFurtherGivenNames();
    	if (more != null) {
    		String[] moreNames = more.split(" ");
    		for (String extraName : moreNames) result.addGiven(extraName);
    	}
    	return result;
    }
    
    /**
     * XDS Address -> FHIR Address
     * @param address
     * @return
     */
    public org.hl7.fhir.r4.model.Address transform(Address address) {
    	org.hl7.fhir.r4.model.Address result = new org.hl7.fhir.r4.model.Address();
    	result.setCity(address.getCity());
    	result.setCountry(address.getCountry());
    	result.setDistrict(address.getCountyParishCode());
    	result.setState(address.getStateOrProvince());
    	result.setPostalCode(address.getZipOrPostalCode());
    	String street = address.getStreetAddress();
    	if (street != null) result.addLine(street);
    	String other = address.getOtherDesignation();
    	if (other != null) result.addLine(other);
    	
    	return result;
    }
    
    /**
     * XDS LocalizedString -> FHIR Narrative
     * @param in
     * @return
     */
    public Narrative transformToNarrative(LocalizedString in) {
    	 if (in==null) return null;
         Narrative result = new Narrative();
         result.setStatus(org.hl7.fhir.r4.model.Narrative.NarrativeStatus.GENERATED);
         result.setDivAsString(in.getValue());
         return result;
    }
    
    /**
     * XDS Identifiable -> FHIR Patient Reference
     * @param patient
     * @return
     */
    public Reference transformPatient(Identifiable patient) {    	
    	String system = patient.getAssigningAuthority().getUniversalId();
    	String value = patient.getId(); 
		return patientReferenceCreator.createPatientReference(system, value);
    }
    
    /**
     * XDS Identifiable -> FHIR CodeableConcept
     * @param patient
     * @return
     */
    public CodeableConcept transform(Identifiable patient) {
        CodeableConcept result = new CodeableConcept();
        Coding coding = result.addCoding();
    	AssigningAuthority assigningAuthority = patient.getAssigningAuthority();
    	String value = patient.getId();
    	if (assigningAuthority != null) {
    	  String system = assigningAuthority.getUniversalId();    	     	
    	  coding.setSystem("urn:oid:"+system);
        } 
    	coding.setCode(value);
		return result;
    }
    
    /**
     * XDS Identifiable -> FHIR Identifier
     * @param identifiable
     * @return
     */
    public Identifier transformToIdentifier(Identifiable identifiable) {
    	if (identifiable == null) return null;
    	Identifier result = new Identifier();        
    	AssigningAuthority assigningAuthority = identifiable.getAssigningAuthority();
    	String value = identifiable.getId();
    	if (assigningAuthority != null) {
    	  String system = assigningAuthority.getUniversalId();    	     	
    	  result.setSystem("urn:oid:"+system);
        } 
    	result.setValue(value);
		return result;
    }
    
    private boolean isPatientAuthor(Author author) {
    	if (author == null) return false;
    	if (author.getAuthorRole()==null) return false;
    	for (Identifiable roles : author.getAuthorRole()) {
    		if ("PAT".equals(roles.getId()) && "2.16.756.5.30.1.127.3.10.6".equals(roles.getAssigningAuthority().getUniversalId())) return true;
    		if ("REP".equals(roles.getId()) && "2.16.756.5.30.1.127.3.10.6".equals(roles.getAssigningAuthority().getUniversalId())) return true;
    	}
    	return false;
    }
    /**
     * XDS Author -> FHIR Reference
     * @param author
     * @return
     */
    public Reference transformAuthor(Author author) {
    	Person person = author.getAuthorPerson();
    	
    	if (isPatientAuthor(author)) {
    		Patient patient = transformPatient(person);
    		Reference result = new Reference();
    		List<Telecom> telecoms = author.getAuthorTelecom();    		
    		for (Telecom telecom : telecoms) patient.addTelecom(transform(telecom));
    		result.setResource(patient);
    		return result;
    	}
    	
		Practitioner containedPerson = transformPractitioner(person);
		PractitionerRole role = null;
	
		List<org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization> orgs = author.getAuthorInstitution();
		List<Identifiable> roles = author.getAuthorRole();
		List<Identifiable> specialities = author.getAuthorSpecialty();
		
		if (!orgs.isEmpty() || !roles.isEmpty() || !specialities.isEmpty()) {
			role = new PractitionerRole();                    			
			if (containedPerson != null) role.setPractitioner((Reference) new Reference().setResource(containedPerson));
		}
		
		for (org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization org : orgs) {
			role.setOrganization((Reference) new Reference().setResource(transform(org)));
		}

		for (Identifiable roleId : roles) {
			role.addCode(transform(roleId));
		}
		
		for (Identifiable specId : specialities) {
			role.addSpecialty(transform(specId));
		}                    		                    		                    	                    		
		
		Reference result = new Reference();
		List<Telecom> telecoms = author.getAuthorTelecom();
		if (role == null) {
		  for (Telecom telecom : telecoms) containedPerson.addTelecom(transform(telecom));
		  result.setResource(containedPerson);
		} else {
		  for (Telecom telecom : telecoms) role.addTelecom(transform(telecom));
		  result.setResource(role);
		}
		return result;
    }

}
