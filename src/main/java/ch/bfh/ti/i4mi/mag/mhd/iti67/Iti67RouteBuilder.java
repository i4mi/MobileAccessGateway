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

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;
import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelValidators.itiRequestValidator;

import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import lombok.extern.slf4j.Slf4j;

/**
 * IHE MHD: Find Document References [ITI-67] for Document Responder
 * https://oehf.github.io/ipf-docs/docs/ihe/iti67/
 */
@Slf4j
@Component
class Iti67RouteBuilder extends RouteBuilder {
    
    private final Config config;

    public Iti67RouteBuilder(final Config config) {
        super();
        log.debug("Iti67RouteBuilder initialized");
        this.config = config;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti67RouteBuilder configure");
        final String xds18Endpoint = String.format("xds-iti18://%s/xds/iti18" +
                "?secure=%s", this.config.getHostUrl(), this.config.isHttps() ? "true" : "false")
                +
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger";
        from("mhd-iti67:translation?audit=false").routeId("mdh-documentreference-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())                
                .bean(Utils.class,"searchParameterToBody")
                .bean(Iti67RequestConverter.class)
                .to(xds18Endpoint)
                .process(translateToFhir(new Iti67ResponseConverter(config) , QueryResponse.class));
    }
}
