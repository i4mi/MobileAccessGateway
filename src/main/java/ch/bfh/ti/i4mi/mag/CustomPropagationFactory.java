package ch.bfh.ti.i4mi.mag;

import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;

import java.util.Arrays;
import java.util.List;

public class CustomPropagationFactory extends Propagation.Factory {

    private static final String TRACE_PARENT = "traceparent";
    private static final String TRACE_STATE = "tracestate";

    @Override
    public <K> Propagation<K> create(Propagation.KeyFactory<K> keyFactory) {
        return new CustomPropagation<>(keyFactory);
    }

    static final class CustomPropagation<K> implements Propagation<K> {
        final K traceParentKey, traceStateKey;

        CustomPropagation(KeyFactory<K> keyFactory) {
            this.traceParentKey = keyFactory.create(TRACE_PARENT);
            this.traceStateKey = keyFactory.create(TRACE_STATE);
        }

        @Override
        public List<K> keys() {
            return Arrays.asList(traceParentKey, traceStateKey);
        }

        @Override
        public <C> TraceContext.Injector<C> injector(Setter<C, K> setter) {
            return (traceContext, carrier) -> {
                setter.put(carrier, traceParentKey, formatTraceParent(traceContext));
                setter.put(carrier, traceStateKey, "");
            };
        }

        @Override
        public <C> TraceContext.Extractor<C> extractor(Getter<C, K> getter) {
            return new TraceContext.Extractor<C>() {
                @Override
                public TraceContextOrSamplingFlags extract(C carrier) {
                    String traceParent = getter.get(carrier, traceParentKey);
                    if (traceParent != null) {
                        TraceContext context = extractFromTraceParent(traceParent);
                        return context != null ? TraceContextOrSamplingFlags.create(context) : TraceContextOrSamplingFlags.EMPTY;
                    }
                    return TraceContextOrSamplingFlags.EMPTY;
                }
            };
        }

        private String formatTraceParent(TraceContext context) {
            return String.format("00-%s-%s-%02x",
                    context.traceIdString(),
                    context.spanIdString(),
                    context.sampledLocal() ? 1 : 0);
        }

        private TraceContext extractFromTraceParent(String traceParent) {
            String[] parts = traceParent.split("-");
            if (parts.length != 4) {
                return null;
            }
            long traceIdHigh = Long.parseUnsignedLong(parts[1].substring(0, 16), 16);
            long traceIdLow = Long.parseUnsignedLong(parts[1].substring(16), 16);
            long spanId = Long.parseUnsignedLong(parts[2], 16);
            boolean sampled = parts[3].equals("01");

            return TraceContext.newBuilder()
                    .traceIdHigh(traceIdHigh)
                    .traceId(traceIdLow)
                    .spanId(spanId)
                    .sampled(sampled)
                    .build();
        }
    }
}