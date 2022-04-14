/*
 * Copyright 2015 the original author or authors.
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

import org.apache.camel.spi.UriEndpoint;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirQueryAuditDataset;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirConsumer;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirEndpoint;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirEndpointConfiguration;

/**
 * Patient Identity Feed FHIR endpoint (ITI-104)
 *
 */
@UriEndpoint(scheme = "pmir-iti104", title = "ITI-104 PMIR", syntax = "pmir-iti104:host:port", consumerClass = FhirConsumer.class, label = "http")
public class Iti104Endpoint extends FhirEndpoint<Iti104AuditDataset, Iti104Component> {

    public Iti104Endpoint(String uri, Iti104Component fhirComponent, FhirEndpointConfiguration<Iti104AuditDataset> config) {
        super(uri, fhirComponent, config);
    }

    @Override
    protected String createEndpointUri() {
        return "pmir-iti104:" + "not-implemented yet";
    }
}
