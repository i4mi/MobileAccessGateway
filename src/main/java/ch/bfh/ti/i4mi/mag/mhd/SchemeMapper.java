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

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * mapper for code systems to schemes
 * @author alexander kreutz
 *
 */
@Service
public class SchemeMapper {

	Map<String, String> schemeToSystem = new HashMap<String, String>();
	Map<String, String> systemToScheme = new HashMap<String, String>();
	
	public SchemeMapper() {
		registerSchemeToSystem("2.16.840.1.113883.6.96", "http://snomed.info/sct");
		registerSchemeToSystem("1.3.6.1.4.1.19376.1.2.3", "http://ihe.net/fhir/ihe.formatcode.fhir/CodeSystem/formatcode");
		registerSchemeToSystem("2.16.840.1.113883.6.1",	"http://loinc.org");
		registerSchemeToSystem("2.16.840.1.113883.5.25", "http://terminology.hl7.org/CodeSystem/v3-Confidentiality");
	}
	
	/**
	 * add a scheme to system mapping
	 * @param scheme
	 * @param system
	 */
	public void registerSchemeToSystem(String scheme, String system) {
		schemeToSystem.put(scheme, system);
		systemToScheme.put(system, scheme);
	}
	
	/**
	 * scheme name -> system
	 * @param scheme
	 * @return
	 */
	public String getSystem(String scheme) {
		String system = schemeToSystem.get(scheme);
		if (system != null) return system;
		return "urn:oid:"+scheme;		
	}
	
	/**
	 * system -> scheme name
	 * @param system
	 * @return
	 */
	public String getScheme(String system) {
		if (system==null) {
			return null;
		}
		String scheme = systemToScheme.get(system);
		if (scheme != null) return scheme;
		if (system.startsWith("urn:oid:")) {
            system = system.substring(8);
        } else if (system.startsWith("urn:uuid:")) {
        	system = system.substring(9);
        }
		return system;
	}
}
