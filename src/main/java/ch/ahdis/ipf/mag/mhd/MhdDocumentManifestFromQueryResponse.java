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
package ch.ahdis.ipf.mag.mhd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
import org.openehealth.ipf.commons.ihe.xds.core.responses.QueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.responses.Status;

public class MhdDocumentManifestFromQueryResponse extends MhdFromQueryResponse {
    
    @Override
    public List<DocumentManifest> translateToFhir(QueryResponse input, Map<String, Object> parameters) {
        ArrayList<DocumentManifest> list = new ArrayList<DocumentManifest>();
        if (input != null && Status.SUCCESS.equals(input.getStatus())) {
            if (input.getSubmissionSets() != null) {
                for (SubmissionSet submissionSet : input.getSubmissionSets()) {
                    DocumentManifest documentManifest = new DocumentManifest();
                    
//                    String uuid = UUID.randomUUID().toString();
                    documentManifest.setId(submissionSet.getEntryUuid()); // FIXME do we need to cache this id in relation to the DocumentManifest itself for 
                    
                    list.add(documentManifest);
                    // limitedMetadata -> meta.profile canonical [0..*]
                    // comment -> text Narrative [0..1]
                    // uniqueId -> masterIdentifier Identifier [0..1] [1..1]
                    if (submissionSet.getUniqueId()!=null) {
                        documentManifest.setMasterIdentifier((new Identifier().setValue("urn:oid:"+submissionSet.getUniqueId())));
                    }
                    
                    // entryUUID -> identifier Identifier [0..*]
                    if (submissionSet.getEntryUuid()!=null) {
                        documentManifest.addIdentifier((new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:"+submissionSet.getEntryUuid())));
                    }
                    // availabilityStatus -> status code {DocumentReferenceStatus} [1..1]
                    //   approved -> status=current Other status values are allowed but are not defined in this mapping to XDS.
                    if (AvailabilityStatus.APPROVED.equals(submissionSet.getAvailabilityStatus())) {
                        documentManifest.setStatus(DocumentReferenceStatus.CURRENT);
                    }
                    
                    // contentTypeCode -> type CodeableConcept [0..1]
                    if (submissionSet.getContentTypeCode()!=null) {
                        documentManifest.setType(transform(submissionSet.getContentTypeCode()));
                    }
                    
                    // patientId -> subject Reference(Patient| Practitioner| Group| Device) [0..1], Reference(Patient)
                    // submissionTime -> created dateTime [0..1]
                    if (submissionSet.getSubmissionTime()!=null) {
                        documentManifest.setCreated(submissionSet.getSubmissionTime().getDateTime().toDate());
                    }

                    // authorInstitution, authorPerson, authorRole, authorSpeciality, authorTelecommunication -> author Reference(Practitioner| PractitionerRole| Organization| Device| Patient| RelatedPerson) [0..*]
                    // intendedRecipient -> recipient Reference(Patient| Practitioner| PractitionerRole| RelatedPerson| Organization) [0..*]
                    // sourceId -> source uri [0..1] [1..1]
                    if (submissionSet.getSourceId()!=null) {
                        documentManifest.setSource("urn:oid:"+submissionSet.getSourceId());
                    }
                    // title -> description string [0..1]
                    // References to DocumentReference Resources representing DocumentEntry objects in the SubmissionSet or List Resources representing Folder objects in the SubmissionSet. -> content Reference(Any) [1..*] Reference( DocumentReference| List)
                    // homeCommunityId -> Note 2: Not Applicable - The Document Sharing metadata element has no equivalent element in the HL7 FHIR; therefore, a Document Source is not able to set these elements, and Document Consumers will not have access to these elements.
                }
            }
        } else {
            // throw error?
        }
        return list;
    }
    
    

}
