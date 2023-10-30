package org.openehealth.ipf.commons.ihe.fhir.iti67_v401;
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

import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.openehealth.ipf.commons.core.URN;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Association;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationLabel;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssociationType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntry;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.SubmissionSet;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.XDSMetaClass;
import org.openehealth.ipf.commons.ihe.xds.core.requests.builder.RegisterDocumentSetBuilder;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.openehealth.ipf.platform.camel.ihe.xds.core.converters.EbXML30Converters;

import ch.bfh.ti.i4mi.mag.mhd.iti65.Iti65RequestConverter;
import lombok.extern.slf4j.Slf4j;

/**
 * ITI-67 DocumentReference Update request converter
 * 
 * @author oliver egger
 *
 */
@Slf4j
public class Iti67RequestUpdateConverter extends Iti65RequestConverter {

  /**
   * ITI-67 Response to ITI-57 request converter
   * 
   * @param searchParameter
   * @return
   */
  public SubmitObjectsRequest convertDocumentReferenceToDocumentEntry(@Body DocumentReference documentReference) {

    SubmissionSet submissionSet = new SubmissionSet();
    submissionSet.setSubmissionTime(new Timestamp(ZonedDateTime.now(), Timestamp.Precision.SECOND));

    Extension source = 
        getExtensionByUrl(documentReference, "https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-sourceId");
    if (source != null && source.getValue() instanceof Identifier) {
      submissionSet.setSourceId(noPrefix(((Identifier) source.getValue()).getValue()));
    }

    Extension designationType = 
        getExtensionByUrl(documentReference, "https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType");
    if (designationType != null && designationType.getValue() instanceof CodeableConcept) {
      submissionSet.setContentTypeCode(transformCodeableConcept((CodeableConcept) designationType.getValue()));
    }

    Extension authorRoleExt = 
        getExtensionByUrl(documentReference, "https://fhir.ch/ig/ch-epr-mhealth/StructureDefinition/ch-ext-author-authorrole");
    if (authorRoleExt != null) {
      Identifiable identifiable = null;
      if (authorRoleExt != null) {
        Coding coding = authorRoleExt.castToCoding(authorRoleExt.getValue());
        if (coding != null) {
          identifiable = new Identifiable(coding.getCode(), new AssigningAuthority(noPrefix(coding.getSystem())));
        }
      }
      submissionSet.setAuthor(transformAuthor(null, null, identifiable));
    }

    RegisterDocumentSetBuilder builder = new RegisterDocumentSetBuilder(true, submissionSet); // TODO should be
                                                                                              // true?
    DocumentEntry entry = new DocumentEntry();
    processDocumentReference(documentReference, entry);

    entry.setLogicalUuid(new URN(UUID.randomUUID()).toString());

    submissionSet.setPatientId(entry.getPatientId());
    submissionSet.assignEntryUuid();
    builder.withDocument(entry);
    builder.withAssociation(createHasMemberAssocationWithOriginalPreviousLabel(submissionSet, entry));

    // Submission contains a DocumentEntry object.
    // The logicalID attribute is present in the DocumentEntry object and has a UUID
    // formatted value.
    // The SubmissionSet to DocumentEntry HasMember Association has a Slot with name
    // PreviousVersion. This Slot has a single value, the version number of the
    // previous version, the one being replaced.

    return EbXML30Converters.convert(builder.build());
  }

  private Association createHasMemberAssocationWithOriginalPreviousLabel(SubmissionSet submissionSet,
      DocumentEntry entry) {
    var assoc = createHasMemberAssocation(entry.getEntryUuid(), submissionSet);
    assoc.setLabel(AssociationLabel.ORIGINAL);
    assoc.setPreviousVersion("1"); // FIXME: how do we get that? maybe we could have more updated versions too
    return assoc;
  }

  private Association createHasMemberAssocation(String entryUuid, SubmissionSet submissionSet) {
    return new Association(AssociationType.HAS_MEMBER, new URN(UUID.randomUUID()).toString(),
        submissionSet.getEntryUuid(), entryUuid);

  }

}
