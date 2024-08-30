package ch.bfh.ti.i4mi.mag.mhd.iti66;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class Iti66Condition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean iti18 = context.getEnvironment().getProperty("mag.xds.iti-18.url") != null;
        boolean allowIti66 = context.getEnvironment().getProperty("mag.mhd.iti66.disable") == null || Boolean.parseBoolean(context.getEnvironment().getProperty("mag.mhd.iti66.disable"))==false;
        return iti18 && allowIti66;
    }
}