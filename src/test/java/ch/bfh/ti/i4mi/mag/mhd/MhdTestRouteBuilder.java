package ch.bfh.ti.i4mi.mag.mhd;

import ch.bfh.ti.i4mi.mag.MagConstants;
import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.SampleData;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Version;
import org.openehealth.ipf.commons.ihe.xds.core.requests.QueryRegistry;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.FindDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Rud
 */
@Component
public class MhdTestRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("xds-iti18://iti18Endpoint")
                .process(XdsCamelValidators.iti18RequestValidator())
                .process(exchange -> {
                    log.info("Received ITI-18 request");
                    QueryRegistry iti18Request = exchange.getIn().getMandatoryBody(QueryRegistry.class);
                    QueryResponse iti18Response = null;

                    if (iti18Request.getQuery() instanceof FindDocumentsQuery) {
                        FindDocumentsQuery query = (FindDocumentsQuery) iti18Request.getQuery();
                        if ("deletion-flag-filtering-test-1".equals(query.getPatientId().getId())) {
                            iti18Response = new QueryResponse(Status.SUCCESS);
                            for (int i = 0; i < 4; ++i) {
                                DocumentEntry documentEntry = SampleData.createDocumentEntry(new Identifiable("deletion-flag-filtering-test-1", new AssigningAuthority("1.2.3.4.5")));
                                documentEntry.assignEntryUuid();
                                documentEntry.setUniqueId("uniqueId" + i);
                                iti18Response.getDocumentEntries().add(documentEntry);
                            }
                            iti18Response.getDocumentEntries().get(0).setExtraMetadata(null);
                            iti18Response.getDocumentEntries().get(1).setExtraMetadata(Map.of(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS, List.of(MagConstants.DeletionStatuses.NOT_REQUESTED)));
                            iti18Response.getDocumentEntries().get(2).setExtraMetadata(Map.of(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS, List.of(MagConstants.DeletionStatuses.REQUESTED)));
                            iti18Response.getDocumentEntries().get(3).setExtraMetadata(Map.of(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS, List.of(MagConstants.DeletionStatuses.PROHIBITED)));
                        }

                    } else if (iti18Request.getQuery() instanceof GetDocumentsQuery) {
                        GetDocumentsQuery query = (GetDocumentsQuery) iti18Request.getQuery();
                        if ("urn:uuid:metadata-update-test-1".equals(query.getLogicalUuid().get(0))) {
                            DocumentEntry documentEntry = SampleData.createDocumentEntry(new Identifiable("metadata-update-test-1", new AssigningAuthority("1.2.3.4.5")));
                            documentEntry.assignEntryUuid();
                            documentEntry.setVersion(new Version("42"));

                            iti18Response = new QueryResponse(Status.SUCCESS);
                            iti18Response.getDocumentEntries().add(documentEntry);
                            exchange.getMessage().setBody(iti18Response);
                        }
                    }

                    if (iti18Response != null) {
                        exchange.getMessage().setBody(iti18Response);
                    } else {
                        throw new Exception("Unknown test parameters");
                    }
                })
                .process(XdsCamelValidators.iti18ResponseValidator())
        ;

        from("xds-iti57://iti57Endpoint")
                .process(XdsCamelValidators.iti57RequestValidator())
                .to("direct:handle-metadata-update")
                .process(XdsCamelValidators.iti57ResponseValidator())
        ;

        from("rmu-iti92://iti92Endpoint")
                .process(XdsCamelValidators.iti92RequestValidator())
                .to("direct:handle-metadata-update")
                .process(XdsCamelValidators.iti92ResponseValidator())
        ;

        from("direct:handle-metadata-update")
                .process(exchange -> {
                    log.info("Received metadata update request");
                    RegisterDocumentSet updateRequest = exchange.getIn().getMandatoryBody(RegisterDocumentSet.class);
                    Response updateResponse = new Response();

                    if ("metadata-update-test-1".equals(updateRequest.getSubmissionSet().getPatientId().getId())) {
                        assertEquals(1, updateRequest.getAssociations().size());
                        assertEquals("42", updateRequest.getAssociations().get(0).getPreviousVersion());

                        assertEquals(1, updateRequest.getDocumentEntries().size());
                        DocumentEntry documentEntry = updateRequest.getDocumentEntries().get(0);
                        assertEquals("43", documentEntry.getVersion().getVersionName());
                        assertEquals(1, documentEntry.getExtraMetadata().size());
                        List<String> deletionStatuses = documentEntry.getExtraMetadata().get(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS);
                        assertEquals(1, deletionStatuses.size());
                        assertEquals(MagConstants.DeletionStatuses.REQUESTED, deletionStatuses.get(0));

                        updateResponse.setStatus(Status.SUCCESS);
                    } else {
                        RegisterDocumentSet request = exchange.getIn().getMandatoryBody(RegisterDocumentSet.class);
                        updateResponse.setStatus((request.getDocumentEntries().get(0).getSize() % 2 == 0) ? Status.SUCCESS : Status.FAILURE);
                    }
                    exchange.getMessage().setBody(updateResponse);
                })
        ;

    }

}
