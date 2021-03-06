/*
 * Copyright 2016 the original author or authors.
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

package org.openehealth.ipf.commons.ihe.fhir.iti66_v401;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hl7.fhir.r4.model.Practitioner;
import org.openehealth.ipf.commons.ihe.fhir.FhirSearchParameters;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @since 3.6
 */
@Builder @ToString
public class Iti66SearchParameters implements FhirSearchParameters {

	@Getter @Setter private TokenParam code;
    @Getter @Setter private DateRangeParam date;
    @Getter @Setter private StringParam sourceFamily;
    @Getter @Setter private StringParam sourceGiven;
    @Getter @Setter private TokenOrListParam designationType;
    @Getter @Setter private TokenOrListParam sourceId;
    @Getter @Setter private TokenOrListParam status;
    @Getter @Setter private TokenParam identifier;
    @Getter @Setter private ReferenceParam patientReference;
    @Getter @Setter private TokenParam patientIdentifier;
    @Getter @Setter private TokenParam _id;
    
    @Getter @Setter private SortSpec sortSpec;
    @Getter @Setter private Set<Include> includeSpec;

    @Getter
    private final FhirContext fhirContext;

    @Override
    public List<TokenParam> getPatientIdParam() {
        if (_id != null)
            return Collections.singletonList(_id);
        if (patientReference != null)
            return Collections.singletonList(patientReference.toTokenParam(fhirContext));

        return Collections.singletonList(patientIdentifier);
    }

    public Iti66SearchParameters setSource(ReferenceAndListParam source) {
        if (source != null) {
        	source.getValuesAsQueryTokens().forEach(param -> {
                var ref = param.getValuesAsQueryTokens().get(0);
                var sourceChain = ref.getChain();
                if (Practitioner.SP_FAMILY.equals(sourceChain)) {
                    setSourceFamily(ref.toStringParam(getFhirContext()));
                } else if (Practitioner.SP_GIVEN.equals(sourceChain)) {
                    setSourceGiven(ref.toStringParam(getFhirContext()));
                }
            });
        }
        return this;
    }
}
