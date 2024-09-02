package ch.bfh.ti.i4mi.mag.common;

import lombok.experimental.UtilityClass;
import org.apache.camel.Exchange;
import org.apache.camel.util.CastUtils;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.platform.camel.ihe.ws.AbstractWsEndpoint;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for SOAP exchanges.
 *
 * @author Quentin Ligier
 **/
@UtilityClass
public class SoapExchanges {

    public static @Nullable String readRequestHttpHeader(final String headerName,
                                                         final Exchange exchange,
                                                         final boolean removeIfFound) {
        final Map<String, String> httpHeaders =
                CastUtils.cast((Map<?, ?>) exchange.getMessage().getHeader(AbstractWsEndpoint.INCOMING_SOAP_HEADERS));

        String value = null;
        if (httpHeaders != null && httpHeaders.containsKey(headerName)) {
            value = httpHeaders.get(headerName);
            if (removeIfFound) {
                httpHeaders.remove(headerName);
            }
        } else {
            final Object authHeader = exchange.getMessage().getHeader(headerName);
            if (authHeader != null) {
                value = authHeader.toString();
                if (removeIfFound) {
                    exchange.getMessage().removeHeader(headerName);
                }
            }
        }
        return value;
    }

    public static void writeResponseHttpHeader(final String headerName,
                                               final String headerValue,
                                               final Exchange exchange) {
        final Map<String, String> outgoingHttpHeaders =
                CastUtils.cast(exchange.getMessage().getHeader(AbstractWsEndpoint.OUTGOING_HTTP_HEADERS,
                                                               HashMap::new, Map.class));
        outgoingHttpHeaders.put(headerName, headerValue);
        exchange.getMessage().setHeader(AbstractWsEndpoint.OUTGOING_HTTP_HEADERS, outgoingHttpHeaders);
    }
}
