package ch.bfh.ti.i4mi.mag.pmir.iti93;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class Iti93Condition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean pix = context.getEnvironment().getProperty("mag.pix.iti-44.url") != null;
        boolean allowIti93 = context.getEnvironment().getProperty("mag.pmir.iti93.disable") == null || Boolean.parseBoolean(context.getEnvironment().getProperty("mag.pmir.iti93.disable"))==false;
        return pix && allowIti93;
    }
}