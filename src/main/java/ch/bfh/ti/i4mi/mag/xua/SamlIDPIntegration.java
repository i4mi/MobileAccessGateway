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


package ch.bfh.ti.i4mi.mag.xua;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.AbstractReloadingMetadataProvider;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLBootstrap;
import org.springframework.security.saml.SAMLDiscovery;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.SAMLWebSSOHoKProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.metadata.MetadataMemoryProvider;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.storage.EmptyStorageFactory;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfile;
import org.springframework.security.saml.websso.ArtifactResolutionProfileBase;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileECPImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


/**
 * Integrate IDP
 * @author alexander kreutz
 *
 */
@Configuration
@EnableWebSecurity
public class SamlIDPIntegration extends WebSecurityConfigurerAdapter implements InitializingBean, DisposableBean {
	
	public final static String DEFAULT_IDP = "default";
	
    @Value("${mag.iua.idp.key-store}")
    private String samlKeystore;
    
    @Value("${mag.iua.idp.key-store-password}")
    private String keystorePass;
    
    @Value("${mag.iua.idp.key-alias}")
    private String keyAlias;
    
    @Value("${mag.iua.idp.tls-key-alias:}")
    private String tlsKeyAlias;
    
    @Value("${mag.iua.idp.sign-key-alias:}")
    private String signKeyAlias;
    
    @Value("${mag.iua.idp.key-password}")
    private String keyPassword;
    
    @Value("${mag.iua.idp.tls-key-password:}")
    private String tlsKeyPassword;
    
    @Value("${mag.iua.idp.sign-key-password:}")
    private String signKeyPassword;

    @Value("${mag.iua.sp.entity-id}")
    private String entityId;
    
    @Value("${mag.baseurl}")
    private String baseUrl;
    
    @Value("${server.servlet.context-path:/}")
    private String context;
	
	 // Initialization of OpenSAML library
    @Bean
    public static SAMLBootstrap sAMLBootstrap() {
        return new MySAMLBootstrap();
    }
    
	 @Override
	 protected void configure(HttpSecurity http) throws Exception {
		       
		 http
         .httpBasic()
               .authenticationEntryPoint(samlEntryPoint());      
          http
     		//.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
     		.addFilterAfter(samlFilter(), BasicAuthenticationFilter.class)
     		.addFilterBefore(samlFilter(), CsrfFilter.class);
		 
			http.authorizeRequests()
			.antMatchers("/login").authenticated()
			.antMatchers("/camel/authorize").authenticated()
			.antMatchers("/camel/token").permitAll()
			.antMatchers("/saml/**").permitAll()
	        .antMatchers("/**").permitAll();
			
		http
    		.logout()
    			.disable();	
		
		http.sessionManagement()		    
	        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
	        .sessionFixation().none();
		
		//http.cors().and().csrf().disable();
		
		http.cors().disable();
		http.csrf().disable();
		
	 }
	 
	 private Timer backgroundTaskTimer;
	 private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager;

		public void init() {
			this.backgroundTaskTimer = new Timer(true);
			this.multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
		}

		public void shutdown() {
			this.backgroundTaskTimer.purge();
			this.backgroundTaskTimer.cancel();
			this.multiThreadedHttpConnectionManager.shutdown();
		}
		
	 @ConfigurationProperties("mag.iua.idps")
	 @Bean(name = "idps")
    public Map<String, IDPConfig> getIDPs() {
        Map<String, IDPConfig> result = new HashMap<String, IDPConfig>();
        IDPConfig defaultIdp = getDefaultIdp();
        if (defaultIdp != null && defaultIdp.getMetadataUrl() != null) result.put(DEFAULT_IDP, defaultIdp);
        return result;
    }

    @ConfigurationProperties("mag.iua.idp")
    @Bean(name = "idp")
    public IDPConfig getDefaultIdp() {
        return new IDPConfig();
    }
	 
	 @Bean
	    public FilterChainProxy samlFilter() throws Exception {
	        List<SecurityFilterChain> chains = new ArrayList<SecurityFilterChain>();
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"),
	                samlEntryPoint()));
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"),
	                samlLogoutFilter()));
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"),
	                metadataDisplayFilter()));
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"),
	                samlWebSSOProcessingFilter()));
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"),
	                samlWebSSOHoKProcessingFilter()));
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"),
	                samlLogoutProcessingFilter()));
	        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/discovery/**"),
	                samlIDPDiscovery()));
	        return new FilterChainProxy(chains);
	    }

	     
	    // Initialization of the velocity engine
	    @Bean
	    public VelocityEngine velocityEngine() {
	        return VelocityFactory.getEngine();
	    }
	 
	    // XML parser pool needed for OpenSAML parsing
	    @Bean(initMethod = "initialize")
	    public StaticBasicParserPool parserPool() {
	        return new StaticBasicParserPool();
	    }
	 
	    @Bean(name = "parserPoolHolder")
	    public ParserPoolHolder parserPoolHolder() {
	        return new ParserPoolHolder();
	    }
	 
	    // Bindings, encoders and decoders used for creating and parsing messages
	    @Bean
	    public HttpClient httpClient() {
	        HttpClient result = new HttpClient(this.multiThreadedHttpConnectionManager);
	        result.getParams().setParameter("http.protocol.cookie-policy",
	                CookiePolicy.BROWSER_COMPATIBILITY);
	        return result;
	    }
	 
	    // SAML Authentication Provider responsible for validating of received SAML
	    // messages
	    @Bean
	    public SAMLAuthenticationProvider samlAuthenticationProvider() {
	        SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
	        samlAuthenticationProvider.setUserDetails(new IDPAssertionDetailsService());
	        samlAuthenticationProvider.setForcePrincipalAsString(false);
	        return samlAuthenticationProvider;
	    }
	 
	    // Provider of default SAML Context
	    @Bean
	    public SAMLContextProviderImpl contextProvider() {
	        String serverName = baseUrl;
	        serverName = baseUrl.substring(baseUrl.indexOf("://") + 3);
	        int p = serverName.indexOf("/");
	        if (p > 0) serverName = serverName.substring(0, p);
	        SAMLContextProviderImpl result = new MySAMLContextProvider(serverName, context);
	        //result.setStorageFactory(new EmptyStorageFactory());
	        return result;
	    }
	 	   	 
	    // Logger for SAML messages and events
	    @Bean
	    public SAMLDefaultLogger samlLogger() {
	        SAMLDefaultLogger log = new SAMLDefaultLogger();
	        log.setLogMessagesOnException(true);
	        log.setLogAllMessages(true);
	        log.setLogErrors(true);
	        return log;
	    }
	 
	    // SAML 2.0 WebSSO Assertion Consumer
	    @Bean
	    public WebSSOProfileConsumer webSSOprofileConsumer() {
	        WebSSOProfileConsumerImpl ret = new WebSSOProfileConsumerImpl();
	        ret.setReleaseDOM(false);
	        return ret;
	    }
	 
	    // SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
	    @Bean
	    public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
	        return new WebSSOProfileConsumerHoKImpl();
	    }
	 
	    // SAML 2.0 Web SSO profile
	    @Bean
	    public WebSSOProfile webSSOprofile() {
	        return new WebSSOProfileImpl();
	    }
	 
	    // SAML 2.0 Holder-of-Key Web SSO profile
	    @Bean
	    public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
	        return new WebSSOProfileConsumerHoKImpl();
	    }
	 
	    // SAML 2.0 ECP profile
	    @Bean
	    public WebSSOProfileECPImpl ecpprofile() {
	        return new WebSSOProfileECPImpl();
	    }
	 
	    @Bean
	    public SingleLogoutProfile logoutprofile() {
	        return new SingleLogoutProfileImpl();
	    }
	 
		    
	    // Central storage of cryptographic keys
	    @Bean	    
	    public KeyManager keyManager() {
	    	  DefaultResourceLoader loader = new DefaultResourceLoader();
	          Resource storeFile = loader
	              .getResource(samlKeystore);
	          String storePass = keystorePass;
	          Map<String, String> passwords = new HashMap<String, String>();

	          passwords.put(keyAlias, keyPassword);

	          for (IDPConfig conf : getIDPs().values()) {
	              passwords.put(conf.getKeyAlias(), conf.getKeyPassword());
	              if (conf.getSignKeyAlias() != null && conf.getSignKeyAlias().length() > 0) {
	                  passwords.put(conf.getSignKeyAlias(), conf.getSignKeyPassword());
	              }
	              if (conf.getTlsKeyAlias() != null && conf.getTlsKeyAlias().length() > 0) {
	                  passwords.put(conf.getTlsKeyAlias(), conf.getTlsKeyPassword());
	              }
	          }

	          // String defaultKey = conf.getKeyAlias();
	          return new JKSKeyManager(storeFile, storePass, passwords, keyAlias);
	    	
	    }
	    
	    @Bean
	    public WebSSOProfileOptions defaultWebSSOProfileOptions() {
	        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
	        webSSOProfileOptions.setIncludeScoping(false);	
	        //webSSOProfileOptions.setForceAuthN(true);
	        webSSOProfileOptions.setPassive(false);
	        return webSSOProfileOptions;
	    }
	 
	    // Entry point to initialize authentication, default values taken from
	    // properties file
	    @Bean
	    public SAMLEntryPoint samlEntryPoint() {
	        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
	        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
	        return samlEntryPoint;
	      
	    }
	    
	    // Setup advanced info about metadata
	    //@Bean
	    public ExtendedMetadata extendedMetadata(String entityId, boolean sp) {
	        IDPConfig conf = getIDPs().get(entityId);

	        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
	        extendedMetadata.setIdpDiscoveryEnabled(false);
	        extendedMetadata.setSigningAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
	        extendedMetadata.setDigestMethodAlgorithm("http://www.w3.org/2001/04/xmlenc#sha512");
	        extendedMetadata.setSignMetadata(true);
	        extendedMetadata.setEcpEnabled(true);
	        extendedMetadata.setLocal(true);
	        extendedMetadata.setAlias(sp ? entityId : entityId + "idp");

	        extendedMetadata.setRequireArtifactResolveSigned(true);
	        if (conf.getTlsKeyAlias() != null && conf.getTlsKeyAlias().length() > 0) {
	            extendedMetadata.setTlsKey(conf.getTlsKeyAlias());
	        }
	        if (conf.getSignKeyAlias() != null && conf.getSignKeyAlias().length() > 0) {
	            extendedMetadata.setSigningKey(conf.getSignKeyAlias());
	        }
	        return extendedMetadata;
	    }
	    
	    // IDP Discovery Service
	    //@Bean
	    public SAMLDiscovery samlIDPDiscovery() {
	        SAMLDiscovery idpDiscovery = new SAMLDiscovery();
	        idpDiscovery.setIdpSelectionPath("/saml/discovery");
	        return idpDiscovery;
	    }
	    
	   // @Value("${mag.iua.idp.metadata-url}")
	   // private String metadataUrl;
	    
	    public ExtendedMetadataDelegate ssoCircleExtendedMetadataProvider(String entityId)
	            throws MetadataProviderException {
	            IDPConfig conf = getIDPs().get(entityId);
	            if (conf == null) return null;
	            String idpSSOCircleMetadataURL = conf.getMetadataUrl();
	            AbstractReloadingMetadataProvider prov;
	            if (idpSSOCircleMetadataURL.startsWith("http:") || idpSSOCircleMetadataURL.startsWith("https:")) {
	                prov = new HTTPMetadataProvider(
	                    this.backgroundTaskTimer, httpClient(), idpSSOCircleMetadataURL);
	            } else if (idpSSOCircleMetadataURL.startsWith("classpath:")) {
	                URL fileUrl = getClass().getResource(idpSSOCircleMetadataURL.substring("classpath:".length()));
	                prov = new FilesystemMetadataProvider(new File(fileUrl.getFile()));
	            } else {
	                prov = new FilesystemMetadataProvider(new File(idpSSOCircleMetadataURL));
	            }
	            prov.setParserPool(parserPool());
	            ExtendedMetadataDelegate extendedMetadataDelegate =
	                new ExtendedMetadataDelegate(prov, extendedMetadata(entityId, false));
	            extendedMetadataDelegate.setMetadataTrustCheck(true);
	            extendedMetadataDelegate.setMetadataRequireSignature(false);
	            backgroundTaskTimer.purge();
	            return extendedMetadataDelegate;
	        }

	    // IDP Metadata configuration - paths to metadata of IDPs in circle of trust
	    // is here
	    // Do no forget to call iniitalize method on providers
	    @Bean
	    @Qualifier("metadata")
	    public CachingMetadataManager metadata() throws MetadataProviderException {
	        List<MetadataProvider> providers = new ArrayList<MetadataProvider>();
	        Map<String, IDPConfig> idps = getIDPs();
	        if (idps != null) {
	            for (Map.Entry<String, IDPConfig> entry : idps.entrySet()) {
	                // Generate metadata for each IDP
	                MetadataProvider prov = ssoCircleExtendedMetadataProvider(entry.getKey());
	                providers.add(prov);

	                // Also generate one SP for each IDP (so that there can be different config for each IDP)
	                providers.add(generateSP(entry.getKey()));

	            }
	        }

	        CachingMetadataManager metadata = new CachingMetadataManager(providers);
	        metadata.setHostedSPName(entityId);
	        metadata.setDefaultIDP(DEFAULT_IDP + "idp");
	        return metadata;
	    }
	    	   
	    public MetadataProvider generateSP(String entityId) throws MetadataProviderException {
	        MetadataGenerator generator = metadataGenerator(entityId);
	        EntityDescriptor descriptor = generator.generateMetadata();
	        ExtendedMetadata extendedMetadata = generator.generateExtendedMetadata();

	        MetadataMemoryProvider memoryProvider = new MetadataMemoryProvider(descriptor);
	        memoryProvider.initialize();
	        MetadataProvider metadataProvider = new ExtendedMetadataDelegate(memoryProvider, extendedMetadata);

	        return metadataProvider;
	    } 
	 
	    
	    // Filter automatically generates default SP metadata
	    //@Bean
	    public MetadataGenerator metadataGenerator(String entityId) {
	        MetadataGenerator metadataGenerator = new MetadataGenerator();
	        IDPConfig conf = getIDPs().get(entityId);

	        if (DEFAULT_IDP.equals(entityId)) {
	            metadataGenerator.setEntityId(entityId);
	        } else {
	            metadataGenerator.setEntityId(entityId + "/alias/" + entityId);
	        }
	        metadataGenerator.setEntityBaseURL(baseUrl);//+(context.equals("/")?"":context));

	        metadataGenerator.setExtendedMetadata(extendedMetadata(entityId, true));
	        metadataGenerator.setIncludeDiscoveryExtension(false);

	        metadataGenerator.setKeyManager(keyManager());
	        Collection<String> bindings = new ArrayList<String>();
	        bindings.add("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact");

	        if (conf.isNoArtifactResolution()) {
	            metadataGenerator.setWantAssertionSigned(false);
	        } else {
	            metadataGenerator.setBindingsSSO(bindings);
	        }

	        return metadataGenerator;


	    }

	    
	    // The filter is waiting for connections on URL suffixed with filterSuffix
	    // and presents SP metadata there
	    @Bean
	    public MetadataDisplayFilter metadataDisplayFilter() {
	        return new MetadataDisplayFilter();	        
	    }
	     
	    // Handler deciding where to redirect user after successful login
	    @Bean
	    public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
	        SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler =
	                new SavedRequestAwareAuthenticationSuccessHandler();
	        successRedirectHandler.setDefaultTargetUrl("/camel/token");
	        return successRedirectHandler;
	    }
	    
		// Handler deciding where to redirect user after failed login
	    @Bean
	    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
		    	SimpleUrlAuthenticationFailureHandler failureHandler =
		    			new SimpleUrlAuthenticationFailureHandler();
		    	failureHandler.setUseForward(false);
		    	failureHandler.setDefaultFailureUrl("/not-authenticated.html");
		    	return failureHandler;
	    }
	     
	    @Bean
	    public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
	        SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter = new SAMLWebSSOHoKProcessingFilter();
	        samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
	        samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
	        samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
	        return samlWebSSOHoKProcessingFilter;
	    }
	    
	    // Processing filter for WebSSO profile messages
	    @Bean
	    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
	        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
	        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
	        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
	        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());	        	       
	        return samlWebSSOProcessingFilter;
	    }
	     	   
	     
	    // Handler for successful logout
	    @Bean
	    public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
	        SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
	        successLogoutHandler.setDefaultTargetUrl("/");
	        return successLogoutHandler;
	    }
	     
	    // Logout handler terminating local session
	    @Bean
	    public SecurityContextLogoutHandler logoutHandler() {
	        SecurityContextLogoutHandler logoutHandler = 
	        		new SecurityContextLogoutHandler();
	        logoutHandler.setInvalidateHttpSession(true);
	        logoutHandler.setClearAuthentication(true);
	        return logoutHandler;
	    }
	 
	    // Filter processing incoming logout messages
	    // First argument determines URL user will be redirected to after successful
	    // global logout
	    @Bean
	    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
	        return new SAMLLogoutProcessingFilter(successLogoutHandler(),
	                logoutHandler());
	    }
	     
	    // Overrides default logout processing filter with the one processing SAML
	    // messages
	    @Bean
	    public SAMLLogoutFilter samlLogoutFilter() {
	        return new SAMLLogoutFilter(successLogoutHandler(),
	                new LogoutHandler[] { logoutHandler() },
	                new LogoutHandler[] { logoutHandler() });
	    }
		
	    // Bindings
	    private ArtifactResolutionProfile artifactResolutionProfile() throws MetadataProviderException {
	        final ArtifactResolutionProfileImpl artifactResolutionProfile = 
	        		new ArtifactResolutionProfileImpl(httpClient());
	        artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding()));
	        MetadataManager metadataManager = metadata();
	        metadataManager.refreshMetadata();
	        artifactResolutionProfile.setMetadata(metadataManager);
	        return artifactResolutionProfile;
	        
	    }
	    
	    @Bean
	    public HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine) throws MetadataProviderException {
	        return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile());
	    }
	 
	    @Bean
	    public HTTPSOAP11Binding soapBinding() {
	        return new HTTPSOAP11Binding(parserPool());
	    }
	    
	    @Bean
	    public HTTPPostBinding httpPostBinding() {
	    		return new HTTPPostBinding(parserPool(), velocityEngine());
	    }
	    
	    @Bean
	    public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
	    		return new HTTPRedirectDeflateBinding(parserPool());
	    }
	    
	    @Bean
	    public HTTPSOAP11Binding httpSOAP11Binding() {
	    	return new HTTPSOAP11Binding(parserPool());
	    }
	    
	    @Bean
	    public HTTPPAOS11Binding httpPAOS11Binding() {
	    		return new HTTPPAOS11Binding(parserPool());
	    }
	    
	    // Processor
		@Bean
		public SAMLProcessorImpl processor() throws MetadataProviderException {
			Collection<SAMLBinding> bindings = new ArrayList<SAMLBinding>();
			bindings.add(artifactBinding(parserPool(), velocityEngine()));
			bindings.add(httpPostBinding());
			bindings.add(httpRedirectDeflateBinding());			
			bindings.add(httpSOAP11Binding());
			bindings.add(httpPAOS11Binding());
			return new SAMLProcessorImpl(bindings);
		}
		
		@Bean
	    @Override
	    public AuthenticationManager authenticationManagerBean() throws Exception {
	        return super.authenticationManagerBean();
	    }
		
		@Override
	    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
	        auth
	            .authenticationProvider(samlAuthenticationProvider());
	    }

	    @Override
	    public void afterPropertiesSet() throws Exception {
	        init();
	    }

	    @Override
	    public void destroy() throws Exception {
	        shutdown();
	    }

}
