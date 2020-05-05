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
import org.openehealth.ipf.commons.ihe.fhir.iti67.Iti67SearchParameters;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * IHE MHD: Find Document References [ITI-67] for Document Responder
 * https://oehf.github.io/ipf-docs/docs/ihe/iti67/
 */
@Slf4j
@Component
class Iti67RouteBuilder extends RouteBuilder {

    public Iti67RouteBuilder() {
        super();
        log.debug("Iti67RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti67RouteBuilder configure");
        from("mhd-iti67:translation?audit=false").routeId("mdh-documentreference-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(Utils.searchParameterToBody())
                // translate, forward, translate back
                .process(translateToFhir(new MhdDocumentReferenceMockTranslator() , Iti67SearchParameters.class));
    }
}
