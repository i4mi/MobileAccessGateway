package ch.bfh.ti.i4mi.mag;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

@Component
public class TraceHeaderLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TraceHeaderLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String traceparent = request.getHeader("traceparent");
        String tracestate = request.getHeader("tracestate");
        String b3 = request.getHeader("b3");
        String b3TraceId = request.getHeader("X-B3-TraceId");
        String b3SpanId = request.getHeader("X-B3-SpanId");

        // Log the headers in a structured format
        logger.info("Trace headers: traceparent={}, tracestate={}, b3={}, X-B3-TraceId={}, X-B3-SpanId={}",
                traceparent, tracestate, b3, b3TraceId, b3SpanId);

        // Add the headers to MDC for logging
        MDC.put("traceparent", traceparent);
        MDC.put("tracestate", tracestate);
        MDC.put("b3", b3);
        MDC.put("X-B3-TraceId", b3TraceId);
        MDC.put("X-B3-SpanId", b3SpanId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC
            MDC.clear();  // This removes all MDC entries
        }
    }
}