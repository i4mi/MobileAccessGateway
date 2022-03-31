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
package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.util.Arrays;
import java.util.List;

import org.openehealth.ipf.commons.ihe.core.IntegrationProfile;
import org.openehealth.ipf.commons.ihe.core.InteractionId;
import org.openehealth.ipf.commons.ihe.fhir.FhirInteractionId;
import org.openehealth.ipf.commons.ihe.fhir.FhirTransactionConfiguration;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirQueryAuditDataset;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Oliver Egger
 */
public class PMIR implements IntegrationProfile {

    @AllArgsConstructor
    public enum Interactions implements FhirInteractionId<Iti93AuditDataset> {

        ITI_93(ITI_93_CONFIG);

        @Getter
        FhirTransactionConfiguration<Iti93AuditDataset> fhirTransactionConfiguration;
    }

    @Override
    public List<InteractionId> getInteractionIds() {
        return Arrays.asList(Interactions.values());
    }

    private static final Iti93TransactionConfiguration ITI_93_CONFIG = new Iti93TransactionConfiguration();
}
