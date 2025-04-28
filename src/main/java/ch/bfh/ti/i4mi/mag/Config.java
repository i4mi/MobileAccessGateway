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

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import javax.servlet.Filter;

import ca.uhn.fhir.rest.server.*;
import ch.bfh.ti.i4mi.mag.fhir.MagCapabilityStatementProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import lombok.Data;


// TODO: max-header-size must be used for http also
/**
 * Configuration for MobileHealthGateway
 * see application.yml
 *
 */
@Slf4j
@Configuration
@Data
public class Config {

    /**
     * baseurl of gateway
     */
    @Value("${mag.baseurl:}")
    private String baseurl;

    @Value("${mag.extpatienturl:}")
    private String extpatienturl;

    /**
     * Own full URL of patient endpoint
     */
    public String getUriFhirEndpoint() { return baseurl+"/fhir"; };


    @Value("${mag.client-ssl.keystore.path:}")
    private String keystore;

    @Value("${mag.client-ssl.keystore.base64:}")
    private String keystoreBase64;

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
    public SSLContextParameters getSSLContext() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        
        KeyStoreParameters ksp = new KeyStoreParameters();
            
        // https://www.baeldung.com/java-keystore
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        log.info("keystore base64 valued: " + (keystoreBase64 != null && !keystoreBase64.trim().isEmpty()));
        if (keystoreBase64 != null && !keystoreBase64.trim().isEmpty()) {
          ks.load(ReadCertificateStream(), keystorePassword.toCharArray());        
          ksp.setKeyStore(ks);
        } else {
          // Keystore file may be found at src/main/resources
          ksp.setResource(keystore); 
          ksp.setPassword(keystorePassword);   
        }

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
        //scp.setCertAlias(certAlias);

        return scp;
        //return new SSLContextParameters();
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

    private InputStream ReadCertificateStream () throws FileNotFoundException {        
        byte[] decodedBytes = Base64.getDecoder().decode(keystoreBase64);
        return  new ByteArrayInputStream(decodedBytes);
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

    @Bean(name = "stsEndpoint")
    public String getStsEndpoint(final @Value("${mag.iua.ap.url}") String assertionEndpointUrl,
                                 final @Value("${mag.iua.ap.wsdl}") String wsdl,
                                 final @Value("${mag.iua.ap.endpoint-name:}") String endpointName,
                                 final @Value("${mag.client-ssl.enabled}") boolean clientSsl) {
        return String.format("cxf://%s?dataFormat=CXF_MESSAGE&wsdlURL=%s&loggingFeatureEnabled=true" +
                                     ((endpointName != null && endpointName.length() > 0) ? "&endpointName=" + endpointName : "") +
                                     "&inInterceptors=#soapResponseLogger" +
                                     "&inFaultInterceptors=#soapResponseLogger" +
                                     "&outInterceptors=#soapRequestLogger" +
                                     "&outFaultInterceptors=#soapRequestLogger" +
                                     (clientSsl ? "&sslContextParameters=#sslContext" : ""),
                             assertionEndpointUrl, wsdl);
    }
}
