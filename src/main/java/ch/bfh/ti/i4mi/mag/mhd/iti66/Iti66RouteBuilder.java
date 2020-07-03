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

package ch.bfh.ti.i4mi.mag.mhd.iti66;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import lombok.extern.slf4j.Slf4j;

/**
 * IHE MHD: Find Document Manifests [ITI-66] for Document Responder
 * https://oehf.github.io/ipf-docs/docs/ihe/iti66/
 */
@Slf4j
@Component
class Iti66RouteBuilder extends RouteBuilder {
    
    private final Config config;

    public Iti66RouteBuilder(final Config config) {
        super();
        this.config = config;
        log.debug("Iti66RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti66RouteBuilder configure");
        final String xds18Endpoint = String.format("xds-iti18://%s/xds/iti18" +
          "?secure=%s", this.config.getHostUrl(), this.config.isHttps() ? "true" : "false")
        +
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger";

        from("mhd-iti66:translation?audit=false").routeId("mdh-documentmanifest-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .bean(Utils.class,"searchParameterToBody")
                .bean(Iti66RequestConverter.class)                
                .to(xds18Endpoint)
                .process(translateToFhir(new Iti66ResponseConverter() , QueryResponse.class));
        }
}