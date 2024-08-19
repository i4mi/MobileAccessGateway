package ch.bfh.ti.i4mi.mag;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TraceHeaderLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TraceHeaderLoggingFilter.class);

    private final Tracer tracer;

    public TraceHeaderLoggingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String traceparent = request.getHeader("traceparent");
        String tracestate = request.getHeader("tracestate");
        String b3 = request.getHeader("b3");

        logger.info("Trace headers: traceparent={}, tracestate={}, b3={}", traceparent, tracestate, b3);

        MDC.put("traceparent", traceparent);
        MDC.put("tracestate", tracestate);
        MDC.put("b3", b3);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}