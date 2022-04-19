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

package ch.bfh.ti.i4mi.mag.pmir.iti104;

import static org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirCamelTranslators.translateToFhir;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.ExpressionAdapter;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.BaseResponseConverter;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import ch.bfh.ti.i4mi.mag.pmir.iti78.Iti78RequestConverter;
import ch.bfh.ti.i4mi.mag.pmir.iti78.Iti78ResponseConverter;
import ch.bfh.ti.i4mi.mag.pmir.iti83.Iti83ResponseConverter;
import ch.bfh.ti.i4mi.mag.xua.AuthTokenConverter;
import lombok.extern.slf4j.Slf4j;
 
/**
 * 
 */
@Slf4j
@Component
class Iti104RouteBuilder extends RouteBuilder {

	private final Config config;
	
	@Autowired
	Iti104ResponseConverter converter;
	
	@Autowired
	Iti78ResponseConverter converter_78;
	
    public Iti104RouteBuilder(final Config config) {
        super();
        this.config = config;
        log.debug("Iti104RouteBuilder initialized");
    }

    @Override
    public void configure() throws Exception {
        log.debug("Iti104RouteBuilder configure");
        
        final String xds44Endpoint = String.format("pixv3-iti44://%s" +
                "?secure=%s", this.config.getIti44HostUrl(), this.config.isPixHttps() ? "true" : "false")
                +
                "&audit=true" +
                "&auditContext=#myAuditContext" +
              //  "&sslContextParameters=#pixContext" +
                "&inInterceptors=#soapResponseLogger" + 
                "&inFaultInterceptors=#soapResponseLogger"+
                "&outInterceptors=#soapRequestLogger" + 
                "&outFaultInterceptors=#soapRequestLogger";
        
   	 final String xds47Endpoint = String.format("pdqv3-iti47://%s" +
             "?secure=%s", this.config.getIti47HostUrl(), this.config.isPixHttps() ? "true" : "false")
             +
             //"&sslContextParameters=#pixContext" +
             "&audit=true" +
             "&auditContext=#myAuditContext" +
             "&inInterceptors=#soapResponseLogger" + 
             "&inFaultInterceptors=#soapResponseLogger"+
             "&outInterceptors=#soapRequestLogger" + 
             "&outFaultInterceptors=#soapRequestLogger";
        
        
        from("pmir-iti104:stub?audit=true&auditContext=#myAuditContext").routeId("iti104-feed")
                // pass back errors to the endpoint
                .errorHandler(noErrorHandler())
                .process(AuthTokenConverter.addWsHeader())
                .process(Utils.keepBody())                
                .bean(Iti104RequestConverter.class)
                .doTry()
                  .to(xds44Endpoint)
                  .process(Utils.keptBodyToHeader())
                  .process(Utils.storePreferHeader())
                  .process(translateToFhir(converter , byte[].class))
                  .choice()
	                    .when(header("Prefer").isEqualToIgnoreCase("return=Representation"))	                    
	                    .process(Utils.keepBody())
	                    .bean(Iti78RequestConverter.class, "fromMethodOutcome")
	                    .to(xds47Endpoint)											
	  			        .process(translateToFhir(converter_78 , byte[].class))
	  			        .process(Iti104ResponseConverter.addPatientToOutcome())
	  			        .endChoice()
                  .end()                     
                 .endDoTry()
            	.doCatch(javax.xml.ws.soap.SOAPFaultException.class)
				  .setBody(simple("${exception}"))
				  .bean(BaseResponseConverter.class, "errorFromException")
				.end();
                
    }

    private class Responder extends ExpressionAdapter {

        @Override
        public Object evaluate(Exchange exchange) {
            Bundle requestBundle = exchange.getIn().getBody(Bundle.class);
            
            Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.TRANSACTIONRESPONSE)
                    .setTotal(requestBundle.getTotal());

            return responseBundle;
        }

    }

}
