package ch.bfh.ti.i4mi.mag;

import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpMessage;

public class Utils {

    /**
     * terminate current spring security session
     * @return
     */
    public static Processor endHttpSession() {
        return exchange -> {
            exchange.getIn(HttpMessage.class).getRequest().getSession().invalidate();
        };
    }
}
