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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenRenewRouteBuilder extends RouteBuilder {

    public static final String RENEW_PATH = "renew";
    
    @Value("${mag.iua.ap.url}")
    private String assertionEndpointUrl;
    
    @Value("${mag.iua.ap.wsdl}")
    private String wsdl;
    
    @Value("${mag.iua.ap.endpoint-name:}")
    private String endpointName;
    
   
    @Value("${mag.client-ssl.enabled}")
    private boolean clientSsl;
        

    @Override
    public void configure() throws Exception {
        
        final String assertionEndpoint = String.format("cxf://%s?dataFormat=CXF_MESSAGE&wsdlURL=%s&loggingFeatureEnabled=true"+
                ((endpointName!=null && endpointName.length()>0) ? "&endpointName="+endpointName : "")+
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger"+
                (clientSsl ? "&sslContextParameters=#sslContext" : ""),
                assertionEndpointUrl, wsdl);
        
        log.info("Assertion-URL: "+assertionEndpoint);
        
        from(String.format("servlet://%s?httpMethodRestrict=POST&matchOnUriPrefix=true", RENEW_PATH))
                .routeId("renewEndpoint")
                .process(SAMLRenewSecurityTokenBuilder.keepRequest())
                .setProperty("oauthrequest").method(TokenRenew.class, "emptyAuthRequest")
                .doTry()                          
                    .bean(AuthRequestConverter.class, "buildAssertionRequestFromToken")
                    .setHeader("assertionRequest", body())
                    .bean(SAMLRenewSecurityTokenBuilder.class, "requestRenewToken")                    
                                      
                    .bean(TokenRenew.class, "buildAssertionRequest")
                    .bean(TokenRenew.class, "keepIdpAssertion")
                    .bean(Iti40RequestGenerator.class, "buildAssertion")            
                    .removeHeaders("*", "scope")
                    .setHeader(CxfConstants.OPERATION_NAME,
                            constant("Issue"))
                    .setHeader(CxfConstants.OPERATION_NAMESPACE,
                            constant("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl"))          
                    .to(assertionEndpoint)      
                    .bean(AssertionExtractor.class)
                    .bean(TokenEndpoint.class, "handleFromIdp") 
                    
                .doCatch(AuthException.class)
                    .setBody(simple("${exception}"))
                    .bean(TokenEndpoint.class, "handleError")
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.status}"))
                .end()
                .removeHeaders("*", Exchange.HTTP_RESPONSE_CODE)
                .setHeader("Cache-Control", constant("no-store"))
                .setHeader("Pragma", constant("no-cache"))
                .marshal()
                .json();
    }
}
