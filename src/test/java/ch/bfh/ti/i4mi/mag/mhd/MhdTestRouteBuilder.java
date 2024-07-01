package ch.bfh.ti.i4mi.mag.mhd;

import org.apache.camel.builder.RouteBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.requests.RegisterDocumentSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Response;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;
import org.openehealth.ipf.platform.camel.ihe.xds.XdsCamelValidators;
import org.springframework.stereotype.Component;

/**
 * @author Dmytro Rud
 */
@Component
public class MhdTestRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("xds-iti57://iti57Endpoint")
                .process(XdsCamelValidators.iti57RequestValidator())
                .process(exchange -> {
                    log.info("Received ITI-57 request");
                    RegisterDocumentSet request = exchange.getIn().getMandatoryBody(RegisterDocumentSet.class);
                    Response response = new Response();
                    response.setStatus((request.getDocumentEntries().get(0).getSize() % 2 == 0) ? Status.SUCCESS : Status.FAILURE);
                    exchange.getMessage().setBody(response);
                })
        ;

    }

}
