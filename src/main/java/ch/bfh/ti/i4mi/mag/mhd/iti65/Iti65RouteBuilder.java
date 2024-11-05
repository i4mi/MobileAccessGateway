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

package ch.bfh.ti.i4mi.mag.mhd.iti65;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import ch.bfh.ti.i4mi.mag.common.RequestHeadersForwarder;
import ch.bfh.ti.i4mi.mag.common.TraceparentHandler;
import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.ebxml.ebxml30.ProvideAndRegisterDocumentSetRequestType;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import lombok.extern.slf4j.Slf4j;

/**
 * IHE MHD: [ITI-65] Provide Document Bundle Request Message for Document Recipient
 * https://oehf.github.io/ipf-docs/docs/ihe/iti65/
 */
@Slf4j
@Component
@ConditionalOnProperty("mag.xds.iti-41.url")
class Iti65RouteBuilder extends RouteBuilder {

	private final Config config;
	
    public Iti65RouteBuilder(final Config config) {
        super();
        this.config = config;
        log.debug("Iti65RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti65RouteBuilder configure");
        
        final String xds41Endpoint = String.format("xds-iti41://%s" +
                "?secure=%s", this.config.getIti41HostUrl(), this.config.isHttps() ? "true" : "false")
              +
                      "&audit=true" +
                      "&auditContext=#myAuditContext" +
                //      "&sslContextParameters=#pixContext" +
                      "&inInterceptors=#soapResponseLogger" + 
                      "&inFaultInterceptors=#soapResponseLogger"+
                      "&outInterceptors=#soapRequestLogger" + 
                      "&outFaultInterceptors=#soapRequestLogger";
        
        from("mhd-iti65-v401:stub?audit=true&auditContext=#myAuditContext&fhirContext=#fhirContext").routeId("mhd-providedocumentbundle")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                //.process(itiRequestValidator())
                .process(RequestHeadersForwarder.checkAuthorization(config.isChMhdConstraints()))
                .process(RequestHeadersForwarder.forward())
                // translate, forward, translate back
                .process(Utils.keepBody())
                .process(Utils.storeBodyToHeader("BundleRequest"))
                .bean(Iti65RequestConverter.class)
                .process(Utils.storeBodyToHeader("ProvideAndRegisterDocumentSet"))
                .convertBodyTo(ProvideAndRegisterDocumentSetRequestType.class)               
                //.process(iti41RequestValidator())
                .to(xds41Endpoint)
                .convertBodyTo(Response.class)
                .process(TraceparentHandler.updateHeaderForFhir())
                .process(translateToFhir(new Iti65ResponseConverter(config) , Response.class));
    }

     /*
    private class Responder extends ExpressionAdapter {

        @Override
        public Object evaluate(Exchange exchange) {
            Bundle requestBundle = exchange.getIn().getBody(Bundle.class);

            Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.TRANSACTIONRESPONSE)
                    .setTotal(requestBundle.getTotal());

            for (Bundle.BundleEntryComponent requestEntry : requestBundle.getEntry()) {
                Bundle.BundleEntryResponseComponent response = new Bundle.BundleEntryResponseComponent()
                        .setStatus("201 Created")
                        .setLastModified(new Date())
                        .setLocation(requestEntry.getResource().getClass().getSimpleName() + "/" + 4711);
                responseBundle.addEntry()
                        .setResponse(response)
                        .setResource(responseResource(requestEntry.getResource()));
            }
            return responseBundle;
        }

    }

    private Resource responseResource(Resource request) {
        if (request instanceof DocumentManifest) {
            return new DocumentManifest().setId(UUID.randomUUID().toString());
        } else if (request instanceof DocumentReference) {
            return new DocumentReference().setId(UUID.randomUUID().toString());
        } else if (request instanceof ListResource) {
            return new ListResource().setId(UUID.randomUUID().toString());
        } else if (request instanceof Binary) {
            return new Binary().setId(UUID.randomUUID().toString());
        } else {
            throw new IllegalArgumentException(request + " is not allowed here");
        }
    }
    */
}
