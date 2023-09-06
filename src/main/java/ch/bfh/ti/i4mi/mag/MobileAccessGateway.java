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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;

import lombok.extern.slf4j.Slf4j;

import javax.validation.spi.ValidationProvider;

/**
 * Main class of the IPF Mobile Access Gateway application.
 *
 * @author Oliver Egger
 */
@SpringBootApplication
@Slf4j
@ComponentScan(basePackages={"ch.bfh.ti.i4mi.mag","org.openehealth.ipf","org.springframework.security.saml"})	
// without it does not work directly with mvn and current snapshot, when running the Pixm query an error is returned   "resourceType": "OperationOutcome", "issue": [ { "severity": "error", "code": "processing", "diagnostics": "Unknown resource type 'Patient' - Server knows how to handle: [StructureDefinition, OperationDefinition]" } ]
// it looks like the META-INF directory is not correct configured that is copied to the output, if it is added in eclipse as on open project to java/main/resources it works without above line 
@EnableAutoConfiguration
public class MobileAccessGateway {
	
    /**
     * Entry point of the IPF application when running as a JAR (e.g. in IntelliJ IDEA).
     *
     * @param args The list of CLI parameters.
     */
    public static void main(final String[] args) {
        log.info("Configuring Mobile Access Gateway");

        final SpringApplication application = new SpringApplication(MobileAccessGateway.class);
        addApplicationStartupHook(application);
        application.run(args);
    }

    /**
     * Adds a hook to the Application Ready event to run some magic.
     *
     * @param application The IPF {@link SpringApplication} instance.
     */
    public static void addApplicationStartupHook(final SpringApplication application) {
        application.addListeners((ApplicationListener<ApplicationReadyEvent>) event -> {
            log.info("Mobile Access Gateway has been configured and has started");
        });
    }
}
