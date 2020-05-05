package ch.ahdis.ipf.mag.mhd;
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

import java.util.Map;

import org.apache.camel.Processor;
import org.openehealth.ipf.commons.ihe.fhir.Constants;
import org.openehealth.ipf.commons.ihe.fhir.FhirSearchParameters;

public class Utils {
    
    public static Processor searchParameterToBody() {
        return exchange -> {
            Map<String, Object> parameters = exchange.getIn().getHeaders();
            FhirSearchParameters searchParameter = (FhirSearchParameters) parameters
                    .get(Constants.FHIR_REQUEST_PARAMETERS);
            exchange.getIn().setBody(searchParameter);
        };
    }

}
