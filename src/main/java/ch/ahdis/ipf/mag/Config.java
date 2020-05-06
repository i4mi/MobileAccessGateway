package ch.ahdis.ipf.mag;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

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

    private String domainMpiOid = "2.999.756.42.2";
    
    
    
    private String uriMagXdsRetrieve = "http://localhost:9091/camel/xdsretrieve";
}
