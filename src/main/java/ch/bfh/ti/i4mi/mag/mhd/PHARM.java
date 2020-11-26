/*
 * Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ch.bfh.ti.i4mi.mag.mhd;

import java.util.Arrays;
import java.util.List;

import org.openehealth.ipf.commons.ihe.core.IntegrationProfile;
import org.openehealth.ipf.commons.ihe.core.InteractionId;
import org.openehealth.ipf.commons.ihe.fhir.FhirInteractionId;
import org.openehealth.ipf.commons.ihe.fhir.FhirTransactionConfiguration;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirQueryAuditDataset;

import ch.bfh.ti.i4mi.mag.mhd.pharm5.Pharm5TransactionConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Oliver Egger
 */
public class PHARM implements IntegrationProfile {

    @AllArgsConstructor
    public enum Interactions implements FhirInteractionId<FhirQueryAuditDataset> {

        PHARM5(PHARM5_CONFIG);

        @Getter
        FhirTransactionConfiguration<FhirQueryAuditDataset> fhirTransactionConfiguration;
    }

    @Override
    public List<InteractionId> getInteractionIds() {
        return Arrays.asList(Interactions.values());
    }

    private static final Pharm5TransactionConfiguration PHARM5_CONFIG = new Pharm5TransactionConfiguration();
}
