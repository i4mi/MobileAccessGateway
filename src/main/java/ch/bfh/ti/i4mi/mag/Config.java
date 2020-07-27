/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.bfh.ti.i4mi.mag;

import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import lombok.Data;

/**
 * Configuration for MobileHealthGateway
 *
 */
@Configuration
@Data
public class Config {

    //g eneral configuration

    
    // PMP configuration
//     private boolean https = true;
//    private String hostUrl = "sct-form.hcuge.ch/sharedtreatmentplan/services";
//    private String patientId = "12345678";
//    private String repositoryUniqueId = "2.999.756.42.1";
//    private String sourceId = "2.999.756.123.1";
//
//    private String submissionSetPatientPerson = "2.999.1.2.3.1375^Welby^Marcus^^MD^Dr";
//    private String submissionSetPatientRole = "PAT^^^&2.16.756.5.30.1.127.3.10.6&ISO";
//    
//    private org.openehealth.ipf.commons.ihe.core.payload.ExpressionResolver resolver;
//
//    private String domainMpiOid = "2.999.756.42.2";
    
    

    // XDSTools7 configuration https://ehealthsuisse.ihe-europe.net/xdstools7/#
    private boolean https = false;
    private boolean pixHttps = true;
    // private String hostUrl = "ehealthsuisse.ihe-europe.net:10443/xdstools7/sim/default__ahdis/reg/sq"; // https
    private String hostUrl = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/sq"; // http
    private String hostUrl43Http = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/rep/ret"; // http
    private String hostUrl41Http = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/rep/prb"; // http
    //private String hostUrl45Http = "gazelle.interopsante.org/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType"; // http
    private String hostUrl45Http = "gazelle.ihe.net/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType"; // http
       
 
    private String uriMagXdsRetrieve = "http://localhost:9091/camel/xdsretrieve";
    private String uriPatientEndpoint = "http://localhost:9091/fhir/Patient";
        
    
    // see https://oehf.github.io/ipf-docs/docs/ihe/wsPayloadLogging
    @Bean
    public OutPayloadLoggerInterceptor soapRequestLogger() {
        return new OutPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-request.txt");
    }

    @Bean
    public InPayloadLoggerInterceptor soapResponseLogger() {
        return new InPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-response.txt");
    }
    
    @Bean
    public SchemeMapper getSchemeMapper() {
    	return new SchemeMapper();
    }
    
    @Bean
    public PatientReferenceCreator getPatientReferenceCreator() {
    	return new PatientReferenceCreator();
    }
    
}
