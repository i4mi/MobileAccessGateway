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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * IUA ITI-71: Route for token exchange
 *
 * @author alexander kreutz
 */
@Component
public class TokenEndpointRouteBuilder extends RouteBuilder {

    public static final String TOKEN_PATH = "token";

    private final String stsEndpoint;

    public TokenEndpointRouteBuilder(final @Qualifier("stsEndpoint") String stsEndpoint) {
        this.stsEndpoint = stsEndpoint;
    }

    @Override
    public void configure() throws Exception {

        // The direct route for the authorization_code grant type
        from("direct:authorization_code")
                .doTry()
                .bean(TokenEndpoint.class, "handle")
                .doCatch(AuthException.class)
                .setBody(simple("${exception}"))
                .bean(TokenEndpoint.class, "handleError")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.status}"))
                .end();

        // The direct route for the refresh_token grant type
        from("direct:refresh_token")
                .routeId("renewEndpoint")
                .process(SAMLRenewSecurityTokenBuilder.keepRequest())
                .setProperty("oauthrequest").method(TokenRenew.class, "emptyAuthRequest")
                .doTry()
                    .bean(AuthRequestConverter.class, "buildAssertionRequestFromToken")
                    .setHeader("assertionRequest", body())
                    // The following bean sends the refresh request to the IDP
                    .bean(SAMLRenewSecurityTokenBuilder.class, "requestRenewToken")
                    // Now we should have the IDP assertion
                    .bean(TokenRenew.class, "buildAssertionRequest")
                    .bean(TokenRenew.class, "keepIdpAssertion")
                    .bean(Iti40RequestGenerator.class, "buildAssertion")
                    .removeHeaders("*", "scope")
                    .setHeader(CxfConstants.OPERATION_NAME,
                               constant("Issue"))
                    .setHeader(CxfConstants.OPERATION_NAMESPACE,
                               constant("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl"))
                    // Doing the Get X-User Assertion call
                    .to(this.stsEndpoint)
                    .bean(XuaUtils.class, "extractAssertionAsString")
                    .bean(TokenEndpoint.class, "generateOAuth2TokenResponse")
                .doCatch(AuthException.class)
                    .setBody(simple("${exception}"))
                    .bean(TokenEndpoint.class, "handleError")
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, simple("${exception.status}"))
                .end();

        // The main route for the token endpoint, dispatching to other direct routes as needed
        from(String.format("servlet://%s?httpMethodRestrict=POST&matchOnUriPrefix=true", TOKEN_PATH))
                .routeId("tokenEndpoint")
                .choice()
                    .when(header("grant_type").isEqualTo("authorization_code"))
                        .log("Received authorization_code request")
                        .to("direct:authorization_code")
                    .when(header("grant_type").isEqualTo("refresh_token"))
                        .log("Received refresh_token request")
                        .to("direct:refresh_token")
                    .otherwise()
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                        .setBody(constant("unsupported_grant_type"))
                    .end()
                .end()
                .removeHeaders("*", Exchange.HTTP_RESPONSE_CODE)
                .setHeader("Cache-Control", constant("no-store"))
                .setHeader("Pragma", constant("no-cache"))
                .marshal()
                .json();
    }
}
