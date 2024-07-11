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
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.GetDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
                    assertInstanceOf(GetDocumentsQuery.class, iti18Request.getQuery());

                    DocumentEntry documentEntry = SampleData.createDocumentEntry(new Identifiable("testIti18-1", new AssigningAuthority("1.2.3.4.5")));
                    documentEntry.assignEntryUuid();
                    documentEntry.setVersion(new Version("42"));

                    QueryResponse iti18Response = new QueryResponse(Status.SUCCESS);
                    iti18Response.getDocumentEntries().add(documentEntry);
                    exchange.getMessage().setBody(iti18Response);
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

                    if ("testIti18-1".equals(updateRequest.getSubmissionSet().getPatientId().getId())) {
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
