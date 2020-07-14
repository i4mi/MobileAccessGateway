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

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
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
				.to(xds45Endpoint)				
				// translate, forward, translate back				
				.process(translateToFhir(converter , byte[].class));
							
	}
}
