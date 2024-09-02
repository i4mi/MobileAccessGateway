package ch.bfh.ti.i4mi.mag.common;

import lombok.experimental.UtilityClass;
import org.apache.camel.Exchange;
import org.apache.camel.util.CastUtils;
import org.openehealth.ipf.commons.ihe.fhir.Constants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for FHIR exchanges.
 *
 * @author Quentin Ligier
 **/
@UtilityClass
public class FhirExchanges {

    public static @Nullable String readRequestHttpHeader(final String headerName,
                                                         final Exchange exchange,
                                                         final boolean removeIfFound) {
        final Map<String, List<String>> httpHeaders =
                CastUtils.cast((Map<?, ?>) exchange.getMessage().getHeader(Constants.HTTP_INCOMING_HEADERS));

        String value = null;
        if (httpHeaders != null) {
            final List<String> header = httpHeaders.get(headerName);
            if (header != null) {
                value = header.get(0);
                if (removeIfFound) {
                    httpHeaders.remove(headerName);
                }
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
                CastUtils.cast(exchange.getMessage().getHeader(Constants.HTTP_OUTGOING_HEADERS,
                                                               HashMap::new, Map.class));
        outgoingHttpHeaders.put(headerName, headerValue);
        exchange.getMessage().setHeader(Constants.HTTP_OUTGOING_HEADERS, outgoingHttpHeaders);
        // TODO: the FHIR consumer does not set the HTTP response headers
    }
}
