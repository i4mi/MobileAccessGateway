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

import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.Filter;

import ca.uhn.fhir.rest.server.*;
import ch.bfh.ti.i4mi.mag.fhir.MagCapabilityStatementProvider;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.DefaultAuditContext;
import org.openehealth.ipf.commons.audit.protocol.TCPSyslogSender;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
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


   /**
    * Use HTTPS for XDS ?
    */
	@Value("${mag.xds.https:true}")
    private boolean https;

    /**
     * home community ID with prefix
     */
    @Value("${mag.homeCommunityId}")
    private String homeCommunity;

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
	
	
	@Value("${mag.xds.iti-57.url:}")
    private String iti57HostUrl;// = "ehealthsuisse.ihe-europe.net:8280/xdstools7/sim/default__ahdis/reg/sq"; // http

    /**
     * URL of CH:PPQ-1 endpoint
     */
    @Value("${mag.ppq.ppq-1.url:}")
    private String ppq1HostUrl;

    /**
     * URL of CH:PPQ-2 endpoint
     */
    @Value("${mag.ppq.ppq-2.url:}")
    private String ppq2HostUrl;

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
	
	@Value("${mag.pix.oids.local-patient-id-aa:}")
	private String localPatientIDAssigningAuthority;
	
    
   /**
    * baseurl of gateway
    */
	@Value("${mag.baseurl:}")
	private String baseurl;

    @Value("${mag.extpatienturl:}")
	private String extpatienturl;

    public String getUriExternalPatientEndpoint() { return extpatienturl; };

	/**
     * Own full URL of patient endpoint
     */
    public String getUriPatientEndpoint() { return baseurl+"/fhir/Patient"; };
   
    public String getUriFhirEndpoint() { return baseurl+"/fhir"; };
    
   
    @Value("${mag.client-ssl.keystore.path:}")
    private String keystore;
    
    @Value("${mag.client-ssl.keystore.password:}")
    private String keystorePassword;

    @Value("${mag.client-ssl.truststore.path:}")
    private String truststore;

    @Value("${mag.client-ssl.truststore.password:}")
    private String truststorePassword;

    
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
    	

        KeyStoreParameters tsp = new KeyStoreParameters();
    	// Keystore file may be found at src/main/resources
    	tsp.setResource(truststore); 
    	tsp.setPassword(truststorePassword);

    	TrustManagersParameters tmp = new TrustManagersParameters();
    	tmp.setKeyStore(tsp);    	
    
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
        //scp.setClientParameters(null);
        //scp.setSessionTimeout("60");
    	return scp;
    }
            
    @Bean(name = "myAuditContext")
    @ConfigurationProperties(prefix = "mag.audit")
    public AuditContext getAuditContext() {
    	DefaultAuditContext context = new DefaultAuditContext();
    	if (this.auditTlsEnabled) {
    	    context.setTlsParameters(new TlsParameterTest(getAuditSSLContext()));
    	}
    	context.setAuditTransmissionProtocol(new TCPSyslogSender());
    	//context.setAuditTransmissionProtocol(new TLSCloseSocket(context.getTlsParameters()));
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
        //context.setSendingApplication("Mobile Access Gateway");
            	    	
        return context;	
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "corsFilterRegistration")
    @ConditionalOnWebApplication
    public FilterRegistrationBean<Filter> corsFilterRegistration() {
        var frb = new FilterRegistrationBean<>();
        // Overwirte cors, otherwise we cannot access /camel/ via javascript
        // need to crosscheck with ch.bfh.ti.i4mi.mag.xuaSamlIDPIntegration
        frb.addUrlPatterns("/fhir/*", "/camel/*");
        frb.setFilter(new CorsFilter(request -> defaultCorsConfiguration()));
        return frb;
    }

    private static CorsConfiguration defaultCorsConfiguration() {
        var cors = new CorsConfiguration();
        cors.addAllowedOrigin("*");
        cors.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        // A comma separated list of allowed headers when making a non simple CORS request.
        cors.setAllowedHeaders(Arrays.asList("Origin", "Accept", "Content-Type",
                "Access-Control-Request-Method", "Access-Control-Request-Headers", "Authorization",
                "Prefer", "If-Match", "If-None-Match", "If-Modified-Since", "If-None-Exist", "Scope"));
        cors.setExposedHeaders(Arrays.asList("Location", "Content-Location", "ETag", "Last-Modified"));
        cors.setMaxAge(300L);
        return cors;
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

    @Bean
    public MagCapabilityStatementProvider serverConformanceProvider(
            final RestfulServer fhirServer,
            @Value("${mag.baseurl}") final String baseUrl) {
        return new MagCapabilityStatementProvider(fhirServer, baseUrl);
    }

    // use to fix https://github.com/i4mi/MobileAccessGateway/issues/56, however we have the CapabilityStatement not filled out anymore
	@Bean
    public RestfulServerConfiguration serverConfiguration() {
        RestfulServerConfiguration config = new RestfulServerConfiguration();
        config.setResourceBindings(new ArrayList<>());
        config.setServerBindings(new ArrayList<>());
        config.setServerAddressStrategy(new HardcodedServerAddressStrategy(getUriFhirEndpoint()));
        return config;
    }
}
