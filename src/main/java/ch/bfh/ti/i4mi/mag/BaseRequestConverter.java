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

package ch.bfh.ti.i4mi.mag;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import net.ihe.gazelle.hl7v3.datatypes.ST;

/**
 * Base class for request converters
 *
 */
public class BaseRequestConverter {

	@Autowired
	private SchemeMapper schemeMapper;// = new SchemeMapper();
	
	@Autowired
	private PatientReferenceCreator patientReferenceCreator;

	public String getScheme(String system) {
		return schemeMapper.getScheme(system);
	}

	public Timestamp timestampFromDateParam(DateParam dateParam) {
		if (dateParam == null)
			return null;
		String dateString = dateParam.getValueAsString();
		dateString = dateString.replaceAll("-", "");
		return Timestamp.fromHL7(dateString);
	}

	public Code codeFromToken(TokenParam param) {
		return new Code(param.getValue(), null, getScheme(param.getSystem()));
	}

	public List<Code> codesFromTokens(TokenOrListParam params) {
		if (params == null)
			return null;
		List<Code> codes = new ArrayList<Code>();
		for (TokenParam token : params.getValuesAsQueryTokens()) {
			codes.add(codeFromToken(token));
		}
		return codes;
	}

	// TODO is this the correct mapping for URIs?
	public List<String> urisFromTokens(TokenOrListParam params) {
		if (params == null)
			return null;
		List<String> result = new ArrayList<String>();
		for (TokenParam token : params.getValuesAsQueryTokens()) {
			result.add(token.getValue());
		}
		return result;
	}

	public ST ST(String text) {
		ST semanticsText = new ST();
		semanticsText.addMixed(text);
		return semanticsText;
	}
	
	public Identifiable transformReference(String targetRef) {
		if (targetRef == null) return null;
		
		Identifiable id = patientReferenceCreator.resolvePatientReference(targetRef);
		if (id!=null) return id;
		
		MultiValueMap<String, String> vals = UriComponentsBuilder.fromUriString(targetRef).build().getQueryParams();
		if (vals.containsKey("identifier")) {
			String[] identifier = vals.getFirst("identifier").split("\\|");
			if (identifier.length == 2) {
				return new Identifiable(identifier[1], new AssigningAuthority(getScheme(identifier[0])));
			}
		}
		return null;
	}
	
	private static volatile long currentId = System.currentTimeMillis();
	
	public String uniqueId() {		
		return Long.toString(currentId++);
	}
	
	
}
