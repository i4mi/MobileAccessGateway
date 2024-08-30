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

package ch.bfh.ti.i4mi.mag.pmir;

import lombok.Setter;
import org.hl7.fhir.r4.model.Reference;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.springframework.beans.factory.annotation.Autowired;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * create a patient reference using the mobile health gateway base address 
 * @author alexander kreutz
 *
 */
@Slf4j
public class PatientReferenceCreator {

	@Autowired
	@Setter
	private Config config;
	
	@Autowired
	@Setter
	private SchemeMapper schemeMapper;
	
	
	/**
	 * create patient reference from identifiable authority and value
	 * @param system
	 * @param value
	 * @return
	 */
	public Reference createPatientReference(String system, String value) {
		Reference result = new Reference();
		//result.setReference(config.getUriPatientEndpoint()+"?identifier=urn:oid:"+system+"|"+value);		
		result.setReference(config.getUriPatientEndpoint()+"/"+createPatientId(system,value));
		return result;
	}
	
	public String createPatientId(String system, String value) {
		if (system.equals(config.getOID_EPRSPID()) &&  config.isChEprspidAsPatientId()) {
			return value;
		}
		return system+"-"+value;
	}
	
	public Identifiable resolvePatientReference(String reference) {
		if (reference.indexOf("Patient/") >= 0) {
			int start = reference.indexOf("Patient/")+"Patient/".length();
			int end = reference.indexOf("?");
			if (end<0) end = reference.length();
			return resolvePatientId(reference.substring(start,end));			
		} else if (reference.indexOf("/")<0) return resolvePatientId(reference);
		return null;
	}
	
	public Identifiable resolvePatientId(String fullId) {
		if (fullId==null) 
			return null;
		int splitIdx = fullId.indexOf("-");
		if (splitIdx>0) {
		  if (fullId.substring(0,splitIdx).contains(".")) {
		    return new Identifiable(fullId.substring(splitIdx+1), new AssigningAuthority(schemeMapper.getScheme(fullId.substring(0,splitIdx))));
		  } else {
		    log.error("expected oid as a system for resolving Patient in: "+fullId);
		  }
		} else {
			if (config.isChEprspidAsPatientId() && fullId.startsWith("76133761")) {
				if (fullId.matches("^[0-9]{18}$") ) {
					return new Identifiable(fullId, new AssigningAuthority(schemeMapper.getScheme(config.getOID_EPRSPID())));
				}
			}
		}
		return null;
	}
}
