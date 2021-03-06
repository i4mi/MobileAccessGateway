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

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Offers an optional additional HTTP server if HTTPS is used
 * @author alexander kreutz
 *
 */
@Configuration
public class HttpServer {

    @Value("${server.http.port:0}")
    private int httpPort;
    
    @Value("${server.max-http-header-size:0}")
    private int maxHttpHeaderSize;
    
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> webServerFactoryCustomizer() {
    	//JettyComponent jettyComponent = getContext().getComponent("jetty", JettyComponent.class);
    	//jettyComponent.setSslContextParameters(scp);
    	
        return new WebServerFactoryCustomizer<JettyServletWebServerFactory>() {

            @Override
            public void customize(JettyServletWebServerFactory factory) {
            	            
                if (httpPort > 0) {
	                factory.addServerCustomizers(new JettyServerCustomizer() {
	
	                    @Override
	                    public void customize(Server server) {
	
	                        ServerConnector httpConnector = new ServerConnector(server);	   	                       
	                        httpConnector.setPort(httpPort);
	                        server.addConnector(httpConnector);
	                        
	                      
	                        if (maxHttpHeaderSize > 0) {
		                        for (ConnectionFactory factory : httpConnector.getConnectionFactories()) {
		                        	if (factory instanceof HttpConfiguration.ConnectionFactory) {
		                				((HttpConfiguration.ConnectionFactory) factory).getHttpConfiguration()
		                						.setRequestHeaderSize(maxHttpHeaderSize);
		                			}
		                        }
	                        }
	                       
	                        	                        	                      
	                    }
	                });
                }
            }
        };
    }
}