package ch.bfh.ti.i4mi.mag;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingSpanHandler extends SpanHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoggingSpanHandler.class);

    public LoggingSpanHandler() {
        logger.info("LoggingSpanHandler constructed");
    }

    @Override
    public boolean begin(TraceContext context, MutableSpan span, TraceContext parent) {
        logger.info("begin method called");
        if (context != null) {
            logger.info("Span started: traceId={}, spanId={}, parentId={}, name={}",
                    context.traceIdString(),
                    context.spanIdString(),
                    parent != null ? parent.spanIdString() : "none",
                    span != null ? span.name() : "null");
        } else {
            logger.info("Span started: context is null");
        }
        return true;
    }

    @Override
    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
        logger.info("end method called");
        if (context != null && span != null) {
            logger.info("Span completed: traceId={}, spanId={}, name={}, duration={}ms, tags={}",
                    context.traceIdString(),
                    context.spanIdString(),
                    span.name(),
                    span.finishTimestamp() - span.startTimestamp(),
                    span.tags());
        } else {
            logger.info("Span completed: context or span is null");
        }
        return true;
    }
}