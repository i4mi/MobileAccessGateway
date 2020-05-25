package ch.bfh.ti.i4mi.mag;

import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class Config {

    private String hostUrl = "sct-form.hcuge.ch/sharedtreatmentplan/services";
    private boolean https = true;
    private String patientId = "12345678";
    private String repositoryUniqueId = "2.999.756.42.1";
    private String sourceId = "2.999.756.123.1";

    private String submissionSetPatientPerson = "2.999.1.2.3.1375^Welby^Marcus^^MD^Dr";
    private String submissionSetPatientRole = "PAT^^^&2.16.756.5.30.1.127.3.10.6&ISO";
    
    private org.openehealth.ipf.commons.ihe.core.payload.ExpressionResolver resolver;

    private String domainMpiOid = "2.999.756.42.2";
    
    
    
    private String uriMagXdsRetrieve = "http://localhost:9091/camel/xdsretrieve";
        
    
    // see https://oehf.github.io/ipf-docs/docs/ihe/wsPayloadLogging
    @Bean
    public OutPayloadLoggerInterceptor soapRequestLogger() {
        return new OutPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-request.txt");
    }

    @Bean
    public InPayloadLoggerInterceptor soapResponseLogger() {
        return new InPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-response.txt");
    }
    
}
