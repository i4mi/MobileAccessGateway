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
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.DocumentManifest;
import org.openehealth.ipf.commons.ihe.fhir.iti66.Iti66SearchParameters;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;

public class MhdDocumentManifestMockTranslator implements ToFhirTranslator<Iti66SearchParameters> {

	@Override
	public List<DocumentManifest> translateToFhir(Iti66SearchParameters input, Map<String, Object> parameters) {
		ArrayList<DocumentManifest> list = new ArrayList<DocumentManifest>();
		DocumentManifest documentManifest = new DocumentManifest();
		documentManifest.setId("id");		
		list.add(documentManifest);
		return list;
	}

}
