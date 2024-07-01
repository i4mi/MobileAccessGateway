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

package ch.bfh.ti.i4mi.mag.mhd.iti67;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;

import java.util.Map;

public class Iti67FromIti57ResponseConverter extends BaseResponseConverter implements ToFhirTranslator<Response> {

	private Config config;

	public Iti67FromIti57ResponseConverter(final Config config) {
		this.config = config;
	}

	/**
	 * convert ITI-57 response to ITI-67 response 
	 */
	@Override
	public Object translateToFhir(Response input, Map<String, Object> parameters) {
		if (input.getStatus().equals(Status.SUCCESS)) {
			MethodOutcome methodOutcome = new MethodOutcome();
			methodOutcome.setResponseStatusCode(200);
			return methodOutcome;
		} else {
			processError(input);
			return null;
		}
	}
}
