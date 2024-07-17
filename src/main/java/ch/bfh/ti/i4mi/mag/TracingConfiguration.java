package ch.bfh.ti.i4mi.mag;

import brave.Tracing;
import brave.propagation.B3Propagation;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

    @Bean
    public Tracing tracing() {
        return Tracing.newBuilder()
                .sampler(Sampler.ALWAYS_SAMPLE)
                .propagationFactory(new CustomPropagationFactory())
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().build())
                .supportsJoin(false)
                .traceId128Bit(true)
                .build();
    }

    @Bean
    public brave.handler.SpanHandler loggingSpanHandler() {
        return new LoggingSpanHandler();
    }
}