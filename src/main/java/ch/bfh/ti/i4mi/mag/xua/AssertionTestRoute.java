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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.DataFormat;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.http.HttpConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Test Route for a IUA to XUA bridge
 * Defined Route is for testing only and does not implement a specification
 *
 */
@Component
public class AssertionTestRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {
									
		from("servlet://token?matchOnUriPrefix=true").routeId("assertionTest")			
	    .bean(AuthRequestConverter.class, "buildRequest")
		.bean(ProvideAssertionBuilder.class, "buildAssertion")
		//.bean(ProvideAssertionBuilder.class, "test")
		.removeHeaders("*")
		.setHeader(CxfConstants.OPERATION_NAME,
		        constant("Issue"))
		.setHeader(CxfConstants.OPERATION_NAMESPACE,
		        constant("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl"))
		//.setHeader("Accept-Encoding", constant("identity"))
		.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, constant(Boolean.TRUE))		
		.to("cxf://https://ehealthsuisse.ihe-europe.net/STS"
			     //+ "?serviceClass=ch.bfh.ti.i4mi.mag.xua.MessageXProvider"
				 + "?dataFormat=CXF_MESSAGE"
			     + "&wsdlURL=https://ehealthsuisse.ihe-europe.net/STS?wsdl")
		//.bean(ProvideAssertionBuilder.class, "test")
		.bean(AssertionExtractor.class);
		
		
	}

	
}
