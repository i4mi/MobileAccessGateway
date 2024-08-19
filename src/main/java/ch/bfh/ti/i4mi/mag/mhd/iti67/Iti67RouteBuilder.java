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

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.bfh.ti.i4mi.mag.MagConstants;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.*;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.xua.AuthTokenConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

/**
 * IHE MHD: Find Document References [ITI-67] for Document Responder
 * https://oehf.github.io/ipf-docs/docs/ihe/iti67/
 */
@Slf4j
@Component
@ConditionalOnProperty({"mag.xds.iti-18.url", "mag.xds.iti-57.url"})
class Iti67RouteBuilder extends RouteBuilder {

    private final Config config;
    private final Iti67ResponseConverter iti67ResponseConverter;
    private final Iti67RequestUpdateConverter iti67RequestUpdateConverter;
    private final Iti67FromIti57ResponseConverter iti67FromIti57ResponseConverter;

    public Iti67RouteBuilder(final Config config,
                             Iti67ResponseConverter iti67ResponseConverter,
                             Iti67RequestUpdateConverter iti67RequestUpdateConverter,
                             Iti67FromIti57ResponseConverter iti67FromIti57ResponseConverter)
    {
        super();
        log.debug("Iti67RouteBuilder initialized");
        this.config = config;
        this.iti67ResponseConverter = iti67ResponseConverter;
        this.iti67RequestUpdateConverter = iti67RequestUpdateConverter;
        this.iti67FromIti57ResponseConverter = iti67FromIti57ResponseConverter;
    }

    private String createEndpointUri(String schema, String partialUrl) {
        return schema + "://" + partialUrl +
                "?secure=" + config.isHttps() +
                "&audit=true" +
                "&auditContext=#myAuditContext" +
                "&inInterceptors=#soapResponseLogger" +
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" +
                "&outFaultInterceptors=#soapRequestLogger";
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti67RouteBuilder configure");
        final String metadataQueryEndpoint  = createEndpointUri("xds-iti18", this.config.getIti18HostUrl());
        final String metadataUpdateEndpoint = createEndpointUri("xds-iti57", this.config.getIti57HostUrl());

        from("mhd-iti67-v401:translation?audit=true&auditContext=#myAuditContext")
                .routeId("mdh-documentreference-adapter")
                .errorHandler(noErrorHandler())
                .process(AuthTokenConverter.addWsHeader())
                .choice()
                .when(header(Constants.FHIR_REQUEST_PARAMETERS).isNotNull())
                .bean(Utils.class,"searchParameterToBody")
                .bean(Iti67RequestConverter.class)
                .to(metadataQueryEndpoint)
                .process(translateToFhir(iti67ResponseConverter, QueryResponse.class))
                .when(PredicateBuilder.and(header("FhirHttpUri").isNotNull(), header("FhirHttpMethod").isEqualTo("GET")))
                .bean(IdRequestConverter.class)
                .to(metadataQueryEndpoint)
                .process(translateToFhir(iti67ResponseConverter, QueryResponse.class))
                .when(PredicateBuilder.and(header("FhirHttpUri").isNotNull(), header("FhirHttpMethod").isEqualTo("PUT")))
                .process(exchange -> {
                    DocumentReference documentReference = exchange.getIn().getMandatoryBody(DocumentReference.class);
                    SubmitObjectsRequest submitObjectsRequest = iti67RequestUpdateConverter.createMetadataUpdateRequest(documentReference);
                    exchange.getMessage().setBody(submitObjectsRequest);
                })
                .to(metadataUpdateEndpoint)
                .process(translateToFhir(iti67FromIti57ResponseConverter, Response.class))
                .when(PredicateBuilder.and(header("FhirHttpUri").isNotNull(), header("FhirHttpMethod").isEqualTo("DELETE")))
                .process(exchange -> {
                    exchange.setProperty("DOCUMENT_ENTRY_LOGICAL_ID", IdRequestConverter.extractId(exchange.getIn().getHeader("FhirHttpUri", String.class)));
                })
                .bean(IdRequestConverter.class)
                .to(metadataQueryEndpoint)
                .process(exchange -> {
                    QueryResponse queryResponse = exchange.getIn().getMandatoryBody(QueryResponse.class);
                    if (queryResponse.getStatus() != Status.SUCCESS) {
                        iti67FromIti57ResponseConverter.processError(queryResponse);
                    }
                    if (queryResponse.getDocumentEntries().isEmpty()) {
                        throw new ResourceNotFoundException(exchange.getProperty("DOCUMENT_ENTRY_LOGICAL_ID", String.class));
                    }
                    if (queryResponse.getDocumentEntries().size() > 1) {
                        throw new InternalErrorException("Expected at most one Document Entry, got " + queryResponse.getDocumentEntries().size());
                    }

                    DocumentEntry documentEntry = queryResponse.getDocumentEntries().get(0);
                    if (documentEntry.getExtraMetadata() == null) {
                        documentEntry.setExtraMetadata(new HashMap<>());
                    }
                    documentEntry.getExtraMetadata().put(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS, List.of(MagConstants.DeletionStatuses.REQUESTED));
                    if (documentEntry.getLogicalUuid() == null) {
                        documentEntry.setLogicalUuid(documentEntry.getEntryUuid());
                    }
                    documentEntry.assignEntryUuid();
                    if (documentEntry.getVersion() == null) {
                        documentEntry.setVersion(new Version("1"));
                    }

                    SubmissionSet submissionSet = iti67RequestUpdateConverter.createSubmissionSet();
                    SubmitObjectsRequest updateRequest = iti67RequestUpdateConverter.createMetadataUpdateRequest(submissionSet, documentEntry);
                    exchange.getMessage().setBody(updateRequest);
                    log.info("Prepared document metadata update request");
                })
                .to(metadataUpdateEndpoint)
                .process(translateToFhir(new Iti67FromIti57ResponseConverter(config), Response.class))
                .end();
    }
}
