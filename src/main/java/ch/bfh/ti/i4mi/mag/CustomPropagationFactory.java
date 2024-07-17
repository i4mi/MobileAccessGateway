package ch.bfh.ti.i4mi.mag;

import brave.propagation.B3Propagation;
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
        return new CustomPropagation<>(B3Propagation.FACTORY.create(keyFactory), keyFactory);
    }

    static final class CustomPropagation<K> implements Propagation<K> {
        final Propagation<K> b3;
        final K traceParentKey, traceStateKey;

        CustomPropagation(Propagation<K> b3, KeyFactory<K> keyFactory) {
            this.b3 = b3;
            this.traceParentKey = keyFactory.create(TRACE_PARENT);
            this.traceStateKey = keyFactory.create(TRACE_STATE);
        }

        @Override
        public List<K> keys() {
            return Arrays.asList(traceParentKey, traceStateKey);
        }

        @Override
        public <C> TraceContext.Injector<C> injector(Setter<C, K> setter) {
            return b3.injector(setter);
        }

        @Override
        public <C> TraceContext.Extractor<C> extractor(Getter<C, K> getter) {
            return new TraceContext.Extractor<C>() {
                @Override
                public TraceContextOrSamplingFlags extract(C carrier) {
                    String traceParent = getter.get(carrier, traceParentKey);
                    if (traceParent != null) {
                        return extractFromTraceParent(traceParent);
                    }
                    return b3.extractor(getter).extract(carrier);
                }
            };
        }

        private TraceContextOrSamplingFlags extractFromTraceParent(String traceParent) {
            String[] parts = traceParent.split("-");
            if (parts.length != 4) {
                return TraceContextOrSamplingFlags.EMPTY;
            }
            long traceIdHigh = Long.parseUnsignedLong(parts[1].substring(0, 16), 16);
            long traceIdLow = Long.parseUnsignedLong(parts[1].substring(16), 16);
            long spanId = Long.parseUnsignedLong(parts[2], 16);
            byte samplingFlags = Byte.parseByte(parts[3], 16);

            return TraceContextOrSamplingFlags.create(TraceContext.newBuilder()
                    .traceIdHigh(traceIdHigh)
                    .traceId(traceIdLow)
                    .spanId(spanId)
                    .sampled(samplingFlags == 1)
                    .build());
        }
    }
}