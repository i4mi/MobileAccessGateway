/*
 * Copyright 2019 the original author or authors.
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

package ch.bfh.ti.i4mi.mag.pmir;

import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.openehealth.ipf.commons.ihe.fhir.SharedFhirProvider;

/**
 * MessageHeaderUriSelector can be used as selector for FHIR Consumers in batch/transaction
 * requests. These requests can be typically handled by a subclass of
 * {@link SharedFhirProvider AbstractBatchTransactionResourceProvider}
 *
 * @author Oliver Egger
 * @since 3.6
 */
public class MessageHeaderUriSelector implements Predicate<Object> {

    private final String eventUri;

    /**
     * @param profileUris Profile URIs expected in the Bundle's meta element
     */
    public MessageHeaderUriSelector(String eventUri) {
        this.eventUri = eventUri;
    }

    /**
     * @param object bundle
     * @return true if eventUri is present in the MessageHeader.eventUri
     */
    @Override
    public boolean test(Object object) {
        Bundle bundle = (Bundle) object;
        if (bundle!=null && bundle.getEntryFirstRep()!=null && bundle.getEntryFirstRep().getResource().getResourceType()!=null) {
            if ("MessageHeader".equals(bundle.getEntryFirstRep().getResource().getResourceType().name())) {
                MessageHeader messageHeader = (MessageHeader) bundle.getEntryFirstRep().getResource();
                return eventUri.equals(messageHeader.getEventUriType().asStringValue());
            }
        }
        return false;
    }
}
