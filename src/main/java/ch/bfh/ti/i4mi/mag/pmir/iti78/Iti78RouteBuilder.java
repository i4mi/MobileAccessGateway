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

package ch.bfh.ti.i4mi.mag.pmir.iti78;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import lombok.extern.slf4j.Slf4j;

/**
 * IHE PDQM: ITI-78 Patient Demographics Query
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.pix.iti-47.url")
public class Iti78RouteBuilder extends RouteBuilder {

	private final Config config;
	
	@Autowired
	Iti78ResponseConverter converter;
	
	public Iti78RouteBuilder(final Config config) {
		super();
	    this.config = config;
		log.debug("Iti78RouteBuilder initialized");
	}

	@Override
	public void configure() throws Exception {
		log.debug("Iti78RouteBuilder configure");
		
		 final String xds47Endpoint = String.format("pdqv3-iti47://%s" +
	                "?secure=%s", this.config.getIti47HostUrl(), this.config.isPixHttps() ? "true" : "false")
	                +
	                //"&sslContextParameters=#pixContext" +
	                "&audit=true" +
	                "&auditContext=#myAuditContext" +
	                "&inInterceptors=#soapResponseLogger" + 
	                "&inFaultInterceptors=#soapResponseLogger"+
	                "&outInterceptors=#soapRequestLogger" + 
	                "&outFaultInterceptors=#soapRequestLogger";
		
		from("pdqm-iti78:translation?audit=true&auditContext=#myAuditContext").routeId("pdqm-adapter")
				// pass back errors to the endpoint
				.errorHandler(noErrorHandler())
				.process(RequestHeadersForwarder.forward())
				.choice()
					.when(header(Constants.FHIR_REQUEST_PARAMETERS).isNotNull())
					   .bean(Iti78RequestConverter.class, "iti78ToIti47Converter")
	                .endChoice()
	                .when(header("FhirHttpUri").isNotNull())
	                   .bean(Iti78RequestConverter.class, "idConverter")
	                .endChoice()
                .end()
				.doTry()
				  .to(xds47Endpoint)
				  .process(TraceparentHandler.updateHeaderForFhir())
			      .process(translateToFhir(converter , byte[].class))
				.doCatch(javax.xml.ws.soap.SOAPFaultException.class)
				  .setBody(simple("${exception}"))
				  .bean(BaseResponseConverter.class, "errorFromException")
				.end();
							
	}
}
