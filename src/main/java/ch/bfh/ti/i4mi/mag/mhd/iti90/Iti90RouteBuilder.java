package ch.bfh.ti.i4mi.mag.mhd.iti90;

import ch.bfh.ti.i4mi.mag.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.BeanScope;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * IHE mCSD: Find Matching Care Services [ITI-90] for Care Services Selective Consumer
 *
 * @author Quentin Ligier
 **/
@Slf4j
@Component
public class Iti90RouteBuilder extends RouteBuilder {

    private final Config config;

    public Iti90RouteBuilder(final Config config) {
        this.config = config;
    }

    @Override
    public void configure() throws Exception {
        final String xds58Endpoint = String.format(
                "hpd-iti58://%s?secure=%s&audit=true&auditContext=#myAuditContext&inInterceptors=#soapResponseLogger&inFaultInterceptors=#soapResponseLogger&outInterceptors=#soapRequestLogger&outFaultInterceptors=#soapRequestLogger",
                this.config.getIti58HostUrl(),
                this.config.isHttps() ? "true" : "false");

        from("mcsd-iti90:mcsd-iti90?audit=true&auditContext=#myAuditContext").routeId("mcsd-iti90-adapter")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                // translate, forward, translate back
                .bean(Iti90RequestConverter.class, "convert", BeanScope.Request)
                .to(xds58Endpoint)
                .bean(Iti90ResponseConverter.class, "convert", BeanScope.Request);
    }
}
