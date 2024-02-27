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
import org.springframework.stereotype.Component;

/**
 * IUA ITI-71: Route for token exchange
 *
 * @author alexander kreutz
 */
@Component
public class TokenEndpointRouteBuilder extends RouteBuilder {

    public static final String TOKEN_PATH = "token";

    @Override
    public void configure() throws Exception {
        from(String.format("servlet://%s?httpMethodRestrict=POST&matchOnUriPrefix=true", TOKEN_PATH))
                .routeId("tokenEndpoint")
                .doTry()
                    .bean(TokenEndpoint.class, "handle")
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
