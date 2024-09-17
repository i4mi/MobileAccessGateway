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

package ch.bfh.ti.i4mi.mag.mhd.pharm5;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.iti67.Iti67ResponseConverter;
import lombok.extern.slf4j.Slf4j;

/**
 * IHE MHD: Use $find-medication-list as PHARM5
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.xds.pharm-5.url")
class Pharm5RouteBuilder extends RouteBuilder {

    private final Config config;

    public Pharm5RouteBuilder(final Config config) {
        super();
        log.debug("Pharm5RouteBuilder initialized");
        this.config = config;
    }

    @Override
    public void configure() throws Exception {
        log.debug("Pharm5RouteBuilder configure");
        final String endpoint = String.format("cmpd-pharm1://%s" +
                "?secure=%s", this.config.getPharm5HostUrl(), this.config.isHttps() ? "true" : "false")
                +
                "&audit=true" +
                "&auditContext=#myAuditContext" +
         //       "&sslContextParameters=#pixContext" +
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger";
        from("mhd-pharm5:translation?audit=true&auditContext=#myAuditContext").routeId("mdh-documentreference-findmedicationlist-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(RequestHeadersForwarder.checkAuthorization(config.isChMhd()))
                .process(RequestHeadersForwarder.forward())
                .bean(Pharm5RequestConverter.class)
                .to(endpoint)
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(translateToFhir(new Iti67ResponseConverter(config) , QueryResponse.class));
    }
}
