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

package ch.bfh.ti.i4mi.mag.pmir.iti104;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Identifier;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirAuditDataset;

import lombok.Getter;
import lombok.Setter;

/**
 * Audit dataset for ITI-104 transaction
 * @author alexander kreutz
 *
 */
public class Iti104AuditDataset extends FhirAuditDataset {

    // Document manifest unique ID
    @Getter @Setter
    private String eventUri;
    
    @Getter @Setter
    private String messageHeaderId;
    
    @Getter
    private List<Identifier> patients = new ArrayList<Identifier>();

    public Iti104AuditDataset() {
        super(true);
    }


   // public void enrichDatasetFromDocumentManifest(DocumentManifest documentManifest) {
        /*var reference = documentManifest.getSubject();
        getPatientIds().add(reference.getResource() != null ?
                reference.getResource().getIdElement().getValue() :
                reference.getReference());
        
        if (!documentManifest.getIdentifier().isEmpty()) {
            this.documentManifestUuid = documentManifest.getIdentifier().get(0).getValue();
        }*/
    //}
}