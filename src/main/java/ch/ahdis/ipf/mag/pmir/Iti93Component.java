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

package ch.ahdis.ipf.mag.pmir;

import static ch.ahdis.ipf.mag.pmir.PMIR.Interactions.ITI_93;

import org.apache.camel.CamelContext;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirQueryAuditDataset;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirComponent;
import org.openehealth.ipf.platform.camel.ihe.fhir.core.FhirEndpointConfiguration;

/**
 * Component for PMIR(ITI-93)
 *
 */
public class Iti93Component extends FhirComponent<FhirQueryAuditDataset> {


    public Iti93Component() {
        super(ITI_93);
    }

    public Iti93Component(CamelContext context) {
        super(context, ITI_93);
    }

    @Override
    protected Iti93Endpoint doCreateEndpoint(String uri, FhirEndpointConfiguration<FhirQueryAuditDataset> config) {
        return new Iti93Endpoint(uri, this, config);
    }

}
