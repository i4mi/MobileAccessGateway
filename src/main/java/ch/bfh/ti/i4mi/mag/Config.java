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

import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.types.AuditSource;
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

  
   // --------------------------------------
   // XDS Configuration
   // ---------------------------------------

   /**
    * Use HTTPS for XDS ?
    */
    private boolean https = false;
    
    /**
     * URL of ITI-18 endpoint (
     */
    private String iti18HostUrl = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/sq"; // http
    
    /**
     * URL of ITI-43 endpoint
     */
    private String iti43HostUrl = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/rep/ret"; // http
    
    /**
     * URL of ITI-41 endpoint
     */
    private String iti41HostUrl = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/rep/prb"; // http
    
    /**
     * Own full URL where clients can retrieve documents from 
     */
    private String uriMagXdsRetrieve = "http://localhost:9091/camel/xdsretrieve";
    
    //private String hostUrl45Http = "gazelle.interopsante.org/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType"; // http
    //private String hostUrl45Http = "gazelle.ihe.net/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType"; // http
    
    
    // ------------------------------------
    // PIX / PMIR Configuration
    // ------------------------------------
    
    /**
     * Use HTTPS for PIX V3?
     */
    private boolean pixHttps = true;
    
    /**
     * URL of ITI-45 endpoint
     */
    private String iti45HostUrl = "ehealthsuisse.ihe-europe.net:10443/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType";
    
    /**
     * URL of ITI-44 endpoint
     */
    private String iti44HostUrl = iti45HostUrl;
    
    /**
     * sender OID used when sending requests
     */
    private String pixMySenderOid = "1.3.6.1.4.1.12559.11.1.2.2.5.7";
    
    /**
     * receiver OID (of target system) used when sending requests
     */
    private String pixReceiverOid = "1.3.6.1.4.1.12559.11.1.2.2.5.11";
    
    /**
     * OID for queries
     */
    private String pixQueryOid = pixMySenderOid;
    
    /**
     * Own full URL of patient endpoint
     */    
    private String uriPatientEndpoint = "http://localhost:9091/fhir/Patient";
    
    /**
     * Connection security : Use client certificate
     */
    @Bean(name = "pixContext")
    public SSLContextParameters getPixSSLContext() {
    	KeyStoreParameters ksp = new KeyStoreParameters();
    	// Keystore file may be found at src/main/resources
    	ksp.setResource("270.jks"); 
    	ksp.setPassword("a1b2c3");    	

    	KeyManagersParameters kmp = new KeyManagersParameters();
    	kmp.setKeyStore(ksp);
    	kmp.setKeyPassword("a1b2c3");   
    	
    	TrustManagersParameters tmp = new TrustManagersParameters();
    	tmp.setKeyStore(ksp);    	
    
    	SSLContextParameters scp = new SSLContextParameters();
    	scp.setKeyManagers(kmp);
    	scp.setTrustManagers(tmp);
    	scp.setCertAlias("gateway");
    	
    	return scp;
    }
    
    @Bean(name = "myAuditContext")
    public DefaultAuditContext getAuditContext() {
    	DefaultAuditContext context = new DefaultAuditContext();    	
    	context.setAuditEnabled(true);
    	context.setAuditSourceId("MySource"); 
        context.setAuditEnterpriseSiteId("MyEnterprise");
        
        context.setAuditRepositoryHost("147.135.232.177");
        context.setAuditRepositoryPort(3001);
        context.setAuditRepositoryTransport("UDP");        
        
        //context.setAuditSource(AuditSource.of("code","system","display"));
        //context.setSendingApplication("MobileAccessGateway");
            	    	
        return context;	
    }
        
    // ---------------------------------------------
    // Logging configuration
    // ---------------------------------------------
    
    // see https://oehf.github.io/ipf-docs/docs/ihe/wsPayloadLogging
    @Bean
    public OutPayloadLoggerInterceptor soapRequestLogger() {
        return new OutPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-request.txt");
    }

    @Bean
    public InPayloadLoggerInterceptor soapResponseLogger() {
        return new InPayloadLoggerInterceptor("./logs/[date('yyyyMMdd-HH00')]/[sequenceId]-soap-response.txt");
    }
    
    // ---------------------------------------------
    // Other beans used
    // ---------------------------------------------
    
    @Bean
    public SchemeMapper getSchemeMapper() {
    	return new SchemeMapper();
    }
    
    @Bean
    public PatientReferenceCreator getPatientReferenceCreator() {
    	return new PatientReferenceCreator();
    }
    
    
}
