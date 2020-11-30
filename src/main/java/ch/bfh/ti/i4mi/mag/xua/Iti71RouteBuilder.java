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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * IUA: ITI-71 Define route for Get-X-User-Assertion
 *
 */
@Component
public class Iti71RouteBuilder extends RouteBuilder {

	@Value("${mag.iua.ap.url}")
	private String assertionEndpointUrl;
	
	@Value("${mag.iua.ap.wsdl}")
	private String wsdl;
	
	@Autowired
	private AuthRequestConverter converter;
	
	@Override
	public void configure() throws Exception {
				
		final String assertionEndpoint = String.format("cxf://%s?dataFormat=CXF_MESSAGE&wsdlURL=%s&loggingFeatureEnabled=true"+
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger",
				assertionEndpointUrl, wsdl);
			
		from("servlet://authorize?matchOnUriPrefix=true").routeId("iti71")	
		.doTry()
		    .setHeader("oauthrequest").method(converter, "buildAuthenticationRequest")
		    .bean(AuthRequestConverter.class, "buildAssertionRequest")
			.bean(Iti40RequestGenerator.class, "buildAssertion")
			
			.removeHeaders("*","oauthrequest")
			.setHeader(CxfConstants.OPERATION_NAME,
			        constant("Issue"))
			.setHeader(CxfConstants.OPERATION_NAMESPACE,
			        constant("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl"))			
			.to(assertionEndpoint)		
			.bean(AssertionExtractor.class)
			.removeHeaders("*","oauthrequest")
			.setHeader("Location").method(AuthResponseConverter.class, "handle")
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(302))
			.removeHeaders("oauthrequest")
			.setBody(constant(null))
		.doCatch(AuthException.class)	
		    .setBody(simple("${exception}"))
			.setHeader("Location").method(AuthResponseConverter.class, "handleerror")
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(302))
			.setBody(constant(null))
		.end();
		
		
	}

	
}
