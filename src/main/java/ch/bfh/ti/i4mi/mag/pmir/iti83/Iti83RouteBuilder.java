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

package ch.bfh.ti.i4mi.mag.pmir.iti83;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateFhir;
import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.iti67.Iti67ResponseConverter;
import ch.bfh.ti.i4mi.mag.pmir.BackTest;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
class Iti83RouteBuilder extends RouteBuilder {

	private final Config config;
	
	@Autowired
	Iti83ResponseConverter converter;
	
	public Iti83RouteBuilder(final Config config) {
		super();
	    this.config = config;
		log.debug("Iti83RouteBuilder initialized");
	}

	@Override
	public void configure() throws Exception {
		log.debug("Iti83RouteBuilder configure");
		
		 final String xds45Endpoint = String.format("pixv3-iti45://%s" +
	                "?secure=%s", this.config.getHostUrl45Http(), this.config.isPixHttps() ? "true" : "false")
	                +
	                "&inInterceptors=#soapResponseLogger" + 
	                "&inFaultInterceptors=#soapResponseLogger"+
	                "&outInterceptors=#soapRequestLogger" + 
	                "&outFaultInterceptors=#soapRequestLogger";
		
		from("pixm-iti83:translation?audit=true").routeId("pixm-adapter")
				// pass back errors to the endpoint
				.errorHandler(noErrorHandler())
				.bean(Iti83RequestConverter.class)
				//.bean(Test.class)
				.to(xds45Endpoint)
				.bean(BackTest.class)
				// translate, forward, translate back				
				.process(translateToFhir(converter , byte[].class));
				
				//.process(translateFhir(new PixmMockTranslator()));


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
