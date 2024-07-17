package ch.bfh.ti.i4mi.mag;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingSpanHandler extends SpanHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoggingSpanHandler.class);

    @Override
    public boolean begin(TraceContext context, MutableSpan span, TraceContext parent) {
        logger.info("Span started: traceId={}, spanId={}, parentId={}",
                context.traceIdString(),
                context.spanIdString(),
                parent != null ? parent.spanIdString() : "none");
        return true;
    }

    @Override
    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
        logger.info("Span completed: traceId={}, spanId={}, name={}, duration={}ms",
                context.traceIdString(),
                context.spanIdString(),
                span.name(),
                span.finishTimestamp() - span.startTimestamp());
        return true;
    }
}