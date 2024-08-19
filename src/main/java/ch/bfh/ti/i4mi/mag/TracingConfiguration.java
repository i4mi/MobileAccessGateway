package ch.bfh.ti.i4mi.mag;

import brave.Tracing;
import brave.handler.SpanHandler;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class TracingConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(TracingConfiguration.class);

    @Bean
    public Tracer tracer(Tracing braveTracing) {
        logger.info("Creating BraveTracer");
        return new BraveTracer(braveTracing.tracer(),
                new BraveCurrentTraceContext(braveTracing.currentTraceContext()),
                new BraveBaggageManager());
    }

    @Bean
    public Tracing braveTracing(SpanHandler spanHandler) {
        logger.info("Creating Tracing with SpanHandler: {}", spanHandler.getClass().getName());
        return Tracing.newBuilder()
                .localServiceName("mobile-access-gateway")
                .supportsJoin(false)
                .traceId128Bit(true)
                .sampler(Sampler.ALWAYS_SAMPLE)
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().build())
                .propagationFactory(new CustomPropagationFactory())
                .addSpanHandler(spanHandler)
                .build();
    }

    @Bean
    public SpanHandler loggingSpanHandler() {
        logger.info("Creating LoggingSpanHandler");
        return new LoggingSpanHandler();
    }
}