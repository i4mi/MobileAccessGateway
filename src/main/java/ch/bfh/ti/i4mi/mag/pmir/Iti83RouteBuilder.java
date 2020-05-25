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

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateFhir;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
class Iti83RouteBuilder extends RouteBuilder {

	public Iti83RouteBuilder() {
		super();
		log.debug("Iti83RouteBuilder initialized");
	}

	@Override
	public void configure() throws Exception {
		log.debug("Iti83RouteBuilder configure");
		from("pixm-iti83:translation?audit=true").routeId("pixm-adapter")
				// pass back errors to the endpoint
				.errorHandler(noErrorHandler())
				// translate, forward, translate back
				.process(translateFhir(new PixmMockTranslator()));

//						new Processor() {
//					public void process(Exchange exchange) throws Exception {

// see general method here						
//						import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateFhir;
//						import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;
//						
//												log.info("processor called");						

//                      PixQueryResponseToPixmResponseTranslator
//						
//				        if (errorField == 3 && errorComponent == 1) {
//				            // Case 3: Patient ID not found
//				            throw Utils.unknownPatientId()
//				        } else if (errorField == 3 && errorComponent == 4) {
//				            // Case 4: Unknown Patient Domain
//				            throw Utils.unknownSourceDomainCode()
//				        } else if (errorField == 4) {
//				            // Case 5: Unknown Target Domain
//				            throw Utils.unknownTargetDomainCode()
//				        } else {
//				            throw Utils.unexpectedProblem()
//												

//					}
//				});
	}
}
