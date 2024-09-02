package ch.bfh.ti.i4mi.mag.common;

import lombok.experimental.UtilityClass;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MobileAccessGateway
 *
 * @author Quentin Ligier
 **/
@UtilityClass
public class TraceparentHandler {
    private static final Logger log = LoggerFactory.getLogger(TraceparentHandler.class);
    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String TRACEPARENT_CAMEL_HEADER = "MAG.Traceparent";

    public static void saveHeader(final Exchange exchange) {
        final var incomingTraceparent = FhirExchanges.readRequestHttpHeader(TRACEPARENT_HEADER, exchange, true);
        if (incomingTraceparent == null) {
            log.info("No traceparent header found (saveHeader)");
            return;
        }
        final Traceparent parsedTraceparent;
        try {
            parsedTraceparent = Traceparent.parse(incomingTraceparent);
        } catch (final Exception e) {
            log.debug("Could not parse traceparent header: {}", incomingTraceparent);
            return;
        }

        exchange.getMessage().setHeader(TRACEPARENT_CAMEL_HEADER, parsedTraceparent);
    }

    public static Processor updateHeaderForSoap() {
        return exchange -> {
            final var traceparent = exchange.getMessage().getHeader(TRACEPARENT_CAMEL_HEADER, Traceparent.class);
            if (traceparent == null) {
                log.info("No traceparent header found (updateHeaderForSoap)");
                return;
            }

            // We need to update the parentId value before forwarding it
            final var newTraceparent = traceparent.withRandomParentId();

            log.debug("Forwarding Traceparent header: {} | The original was: {}", newTraceparent, traceparent);
            SoapExchanges.writeResponseHttpHeader(TRACEPARENT_HEADER, newTraceparent.toString(), exchange);
        };
    }

    public static Processor updateHeaderForFhir() {
        return exchange -> {
            final var traceparent = exchange.getMessage().getHeader(TRACEPARENT_CAMEL_HEADER, Traceparent.class);
            if (traceparent == null) {
                log.info("No traceparent header found (updateHeaderForFhir)");
                return;
            }

            // We need to update the parentId value before forwarding it
            final var newTraceparent = traceparent.withRandomParentId();

            log.debug("Forwarding Traceparent header: {} | The original was: {}", newTraceparent, traceparent);
            FhirExchanges.writeResponseHttpHeader(TRACEPARENT_HEADER, newTraceparent.toString(), exchange);
        };
    }
}
