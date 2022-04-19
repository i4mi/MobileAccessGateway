/*
 * Copyright 2015 the original author or authors.
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

package ch.bfh.ti.i4mi.mag.pmir.iti104;

import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBException;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Patient.LinkType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

/**
 * ITI-104 to ITI-44 request converter
 * @author alexander kreutz
 *
 */
public class Iti104RequestConverter extends Iti104MergeRequestConverter {

	
	private static String decode(String in) {
		return java.net.URLDecoder.decode(in, StandardCharsets.UTF_8);
	}
	
	public static Identifier identifierFromQuery(String query) {
		if (query.startsWith("identifier=")) {
			String tokenParam = query.substring(11);
			String tokenParamDecoded = decode(tokenParam);
			int index = tokenParamDecoded.indexOf("|");
			Identifier identifier = new Identifier();
			identifier.setSystem(tokenParamDecoded.substring(0, index));
			identifier.setValue(tokenParamDecoded.substring(index+1));
			return identifier;
		} else return null;
	}

    /**
     * convert ITI-104 to ITI-44 request
     * @param requestBundle
     * @return
     * @throws JAXBException
     */
	public String iti104ToIti44Converter(@Body Patient patient, @Header("FhirHttpQuery") String query, @Header("FhirHttpMethod") String method) throws JAXBException {
		Identifier identifier = identifierFromQuery(query);
		if (identifier != null) {			
			if (patient.hasLink()) {
				for (var link : patient.getLink()) {
					if(Patient.LinkType.REPLACEDBY.equals(link.getType())) {
						return doMerge(patient, identifier, link.getOther());
					}
				}
			}
			return doCreate(patient, identifier);
		}
		throw new InvalidRequestException("missing conditional update query");
	}
	
	
}
