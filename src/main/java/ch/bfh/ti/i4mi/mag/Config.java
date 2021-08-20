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
import org.openehealth.ipf.commons.audit.CustomTlsParameters;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.types.AuditSource;
import org.openehealth.ipf.commons.ihe.ws.cxf.audit.AbstractAuditInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import ch.bfh.ti.i4mi.mag.mhd.SchemeMapper;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import lombok.Data;


// TODO: max-header-size must be used for http also
/**
 * Configuration for MobileHealthGateway
 * see application.yml
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
	@Value("${mag.xds.https:true}")
    private boolean https;
    
    /**
     * URL of ITI-18 endpoint (
     */
	@Value("${mag.xds.iti-18.url:}")
    private String iti18HostUrl;// = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/sq"; // http
    /**
     * URL of ITI-18 endpoint (
     */
    @Value("${mag.xds.pharm-5.url:}")
    private String pharm5HostUrl;// = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/sq"; // http
    /**
     * URL of ITI-43 endpoint
     */
	@Value("${mag.xds.iti-43.url:}")
    private String iti43HostUrl;// = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/rep/ret"; // http
    
    /**
     * URL of ITI-41 endpoint
     */
	@Value("${mag.xds.iti-41.url:}")
    private String iti41HostUrl;// = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/rep/prb"; // http
    
    /**
     * Own full URL where clients can retrieve documents from 
     */
	@Value("${mag.xds.retrieve.url:}")
    private String uriMagXdsRetrieve;// = "https://localhost:9091/camel/xdsretrieve";
    
	@Value("${mag.xds.retrieve.repositoryUniqueId:}")
	private String repositoryUniqueId;
	
    //private String hostUrl45Http = "gazelle.interopsante.org/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType"; // http
    //private String hostUrl45Http = "gazelle.ihe.net/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType"; // http
    
    
    // ------------------------------------
    // PIX / PMIR Configuration
    // ------------------------------------
    
    /**
     * Use HTTPS for PIX V3?
     */
	@Value("${mag.pix.https:true}")
    private boolean pixHttps;// = true;
    
    /**
     * URL of ITI-45 endpoint
     */
	@Value("${mag.pix.iti-45.url:}")
    private String iti45HostUrl;// = "ehealthsuisse.ihe-europe.net:10443/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType";
    
    /**
     * URL of ITI-44 endpoint
     */
	@Value("${mag.pix.iti-44.url:}")
    private String iti44HostUrl;// = iti45HostUrl;
	
	/**
	 * URL of ITI-47 endpoint
	 */
	@Value("${mag.pix.iti-47.url:}")
    private String iti47HostUrl;
    
    /**
     * sender OID used when sending requests
     */
	@Value("${mag.pix.oids.sender:}")
    private String pixMySenderOid;// = "1.3.6.1.4.1.12559.11.1.2.2.5.7";
    
    /**
     * receiver OID (of target system) used when sending requests
     */
	@Value("${mag.pix.oids.receiver:}")
    private String pixReceiverOid;// = "1.3.6.1.4.1.12559.11.1.2.2.5.11";
    
    /**
     * oid of MPI-PID (Master Patient indext oid in affinity domain this mobile access gateway is configured)
     */
    @Value("${mag.pix.oids.mpi-pid:}")
    private String oidMpiPid;

    final public String OID_EPRSPID = "2.16.756.5.30.1.127.3.10.3";
    /**
     * OID for queries
     */
	@Value("${mag.pix.oids.query:}")
    private String pixQueryOid;// = pixMySenderOid;
	
	@Value("${mag.pix.oids.custodian:}")
	private String custodianOid;
	
    
   /**
    * baseurl of gateway
    */
	@Value("${mag.baseurl:}")
	private String baseurl;
	 
	/**
     * Own full URL of patient endpoint
     */
    public String getUriPatientEndpoint() { return baseurl+"/fhir/Patient"; };
   
    
   
    @Value("${mag.client-ssl.key-store:}")
    private String keystore;
    
    @Value("${mag.client-ssl.key-store-password:}")
    private String keystorePassword;
    
    @Value("${mag.client-ssl.cert-alias:}")
    private String certAlias;
    
    @Value("${mag.audit.audit-tls-enabled:false}")
    private boolean auditTlsEnabled;

    /**
     * Connection security : Use client certificate
     */    
    @Bean(name = "sslContext")
    @ConditionalOnProperty(
    	    value="mag.client-ssl.enabled", 
    	    havingValue = "true", 
    	    matchIfMissing = false)
    public SSLContextParameters getPixSSLContext() {
    	KeyStoreParameters ksp = new KeyStoreParameters();
    	// Keystore file may be found at src/main/resources
    	ksp.setResource(keystore); 
    	ksp.setPassword(keystorePassword);    	

    	KeyManagersParameters kmp = new KeyManagersParameters();
    	kmp.setKeyStore(ksp);
    	kmp.setKeyPassword(keystorePassword);       	
    	
    	TrustManagersParameters tmp = new TrustManagersParameters();
    	tmp.setKeyStore(ksp);    	
    
    	SSLContextParameters scp = new SSLContextParameters();
    	scp.setKeyManagers(kmp);
    	scp.setTrustManagers(tmp);
    	scp.setCertAlias(certAlias);
    
    	return scp;
    }
       
    public SSLContextParameters getAuditSSLContext() {
    	KeyStoreParameters ksp = new KeyStoreParameters();
    	// Keystore file may be found at src/main/resources
    	ksp.setResource(keystore); 
    	ksp.setPassword(keystorePassword);    	

    	KeyManagersParameters kmp = new KeyManagersParameters();
    	kmp.setKeyStore(ksp);
    	kmp.setKeyPassword(keystorePassword);       	
    	
    	TrustManagersParameters tmp = new TrustManagersParameters();
    	tmp.setKeyStore(ksp);    	
    
    	SSLContextParameters scp = new SSLContextParameters();
    	scp.setKeyManagers(kmp);
    	scp.setTrustManagers(tmp);
    	scp.setCertAlias(certAlias);
    
    	return scp;
    }
       
    @Bean(name = "myAuditContext")
    @ConfigurationProperties(prefix = "mag.audit")
    public DefaultAuditContext getAuditContext() {
    	DefaultAuditContext context = new DefaultAuditContext();
    	if (this.auditTlsEnabled) {
    	    context.setTlsParameters(new TlsParameterTest(getAuditSSLContext()));
    	}
    	//CustomTlsParameters p = new CustomTlsParameters();
    	
    	//p.setKeyStoreFile("270.jks");
    	//p.setKeyStorePassword("a1b2c3");
    	//p.setCertAlias("gateway");
    	 
    
    	//context.setTlsParameters(p);
    	/*context.setAuditEnabled(true);
    	context.setAuditSourceId("CCC_BFH_MAG"); 
        context.setAuditEnterpriseSiteId("BFH");
        
        context.setAuditRepositoryHost("147.135.232.177");
        context.setAuditRepositoryPort(3001);
        context.setAuditRepositoryTransport("UDP");        
        */
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
