package ch.bfh.ti.i4mi.mag.mhd;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SchemeMapper {

	Map<String, String> schemeToSystem = new HashMap<String, String>();
	Map<String, String> systemToScheme = new HashMap<String, String>();
	
	public SchemeMapper() {
		registerSchemeToSystem("2.16.840.1.113883.6.96", "http://snomed.info/sct");
	}
	
	public void registerSchemeToSystem(String scheme, String system) {
		schemeToSystem.put(scheme, system);
		systemToScheme.put(system, scheme);
	}
	
	public String getSystem(String scheme) {
		String system = schemeToSystem.get(scheme);
		return system == null ? scheme : system;
	}
	
	public String getScheme(String system) {
		String scheme = systemToScheme.get(system);
		return scheme == null ? system : scheme;
	}
}
