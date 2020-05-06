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

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.fhir.iti66.Iti66SearchParameters;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * IHE MHD: Find Document Manifests [ITI-66] for Document Responder
 * https://oehf.github.io/ipf-docs/docs/ihe/iti66/
 */
@Slf4j
@Component
class Iti66RouteBuilder extends RouteBuilder {

    public Iti66RouteBuilder() {
        super();
        log.debug("Iti66RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti66RouteBuilder configure");
        from("mhd-iti66:translation?audit=false").routeId("mdh-documentmanifest-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(Utils.searchParameterToBody())
                // translate, forward, translate back
                .process(translateToFhir(new MhdDocumentManifestMockTranslator(), Iti66SearchParameters.class));
    }
}
