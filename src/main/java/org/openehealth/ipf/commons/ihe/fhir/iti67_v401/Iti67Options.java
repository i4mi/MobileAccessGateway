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
package org.openehealth.ipf.commons.ihe.fhir.iti67_v401;

import org.openehealth.ipf.commons.ihe.fhir.FhirProvider;
import org.openehealth.ipf.commons.ihe.fhir.FhirTransactionOptions;
import org.openehealth.ipf.commons.ihe.fhir.iti67_v401.Iti67ResourceProvider;
import org.openehealth.ipf.commons.ihe.fhir.iti67_v401.Iti67StrictResourceProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @since 4.1
 */
public enum Iti67Options implements FhirTransactionOptions {

    STRICT(Iti67StrictResourceProvider.class),
    LENIENT(Iti67ResourceProvider.class);

    private final List<Class<? extends FhirProvider>> resourceProviders;

    @SafeVarargs
    Iti67Options(Class<? extends FhirProvider>... resourceProviders) {
        this.resourceProviders = Arrays.asList(resourceProviders);
    }

    @Override
    public List<Class<? extends FhirProvider>> getSupportedThings() {
        return resourceProviders;
    }
}
