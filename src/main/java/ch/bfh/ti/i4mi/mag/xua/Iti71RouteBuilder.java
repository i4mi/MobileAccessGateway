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
import org.apache.cxf.binding.soap.SoapFault;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.mhd.Utils;

/**
 * IUA: ITI-71 Define route for Get-X-User-Assertion
 *
 */
@Component
public class Iti71RouteBuilder extends RouteBuilder {

	public static final String AUTHORIZE_PATH = "authorize";

	private final AuthRequestConverter converter;

	private final String stsEndpoint;

	public Iti71RouteBuilder(final AuthRequestConverter converter,
							 final @Qualifier("stsEndpoint") String stsEndpoint) {
		this.converter = converter;
		this.stsEndpoint = stsEndpoint;
	}
	
	@Override
	public void configure() throws Exception {
		from(String.format("servlet://%s?matchOnUriPrefix=true", AUTHORIZE_PATH)).routeId("iti71")
		.doTry()
		    .setProperty("oauthrequest").method(converter, "buildAuthenticationRequest")
		    
		    // end spring security session in order to prevent use of already expired
		    // identity provider assertions cached in spring security session
		    // this is unrelated to the IDP provider cookie set by the IDP itself
		    .process(Utils.endHttpSession())
		    
		    .bean(AuthRequestConverter.class, "buildAssertionRequest")
		    .bean(TokenRenew.class, "keepIdpAssertion")
			.bean(Iti40RequestGenerator.class, "buildAssertion")			
			.removeHeaders("*","oauthrequest")
			.setHeader(CxfConstants.OPERATION_NAME,
			        constant("Issue"))
			.setHeader(CxfConstants.OPERATION_NAMESPACE,
			        constant("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl"))			
			.to(this.stsEndpoint)
			.bean(XuaUtils.class, "extractAssertionAsString")
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
		.doCatch(SoapFault.class)
			.setBody(simple("${exception}"))
			.setHeader("Location").method(AuthResponseConverter.class, "handlesoaperror")
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(302))
			.setBody(constant(null))
		.end();
	}
}
