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
package ch.ahdis.ipf.mag.mhd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;

public class MhdDocumentReferenceMockTranslator implements ToFhirTranslator<Iti67SearchParameters> {

	@Override
	public List<DocumentReference> translateToFhir(Iti67SearchParameters input, Map<String, Object> parameters) {
		ArrayList<DocumentReference> list = new ArrayList<DocumentReference>();
		DocumentReference documentReference = new DocumentReference();
		documentReference.setId("id");		
		list.add(documentReference);
		return list;
	}

}
