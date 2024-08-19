package ch.bfh.ti.i4mi.mag;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.tracing.Tracer;

@Configuration
public class FilterConfig {

    private final Tracer tracer;

    public FilterConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    @Bean
    public FilterRegistrationBean<TraceHeaderLoggingFilter> loggingFilter() {
        FilterRegistrationBean<TraceHeaderLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceHeaderLoggingFilter(tracer));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestIdFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}