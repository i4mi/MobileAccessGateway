package ch.bfh.ti.i4mi.mag.pmir;

import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;

public class PatientReferenceCreator {

	@Autowired
	Config config;
	
	public Reference createPatientReference(String system, String value) {
		Reference result = new Reference();
		result.setReference(config.getUriPatientEndpoint()+"?identifier=urn:oid:"+system+"|"+value);		
		return result;
	}
}
