package ch.bfh.ti.i4mi.mag.mhd.iti67;
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
import java.util.HashMap;
import java.util.UUID;

import ch.bfh.ti.i4mi.mag.MagConstants;
import org.apache.camel.Body;
import org.hl7.fhir.r4.model.*;
import org.openehealth.ipf.commons.core.URN;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.*;
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
    } else {
      // todo: use Config.getDocumentSourceId
      submissionSet.setSourceId("2.16.756.5.30.1.190.0.0.12.2.101.32");
    }

    Extension designationType = 
        getExtensionByUrl(documentReference, "https://profiles.ihe.net/ITI/MHD/StructureDefinition/ihe-designationType");
    if (designationType != null && designationType.getValue() instanceof CodeableConcept) {
      submissionSet.setContentTypeCode(transformCodeableConcept((CodeableConcept) designationType.getValue()));
    } else {
      submissionSet.setContentTypeCode(new Code("71388002", new LocalizedString("Procedure"), "2.16.840.1.113883.6.96"));
    }

    Extension authorRoleExt =
        getExtensionByUrl(documentReference, MagConstants.FhirExtensionUrls.CH_AUTHOR_ROLE);
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
    entry.setExtraMetadata(new HashMap<>());
    processDocumentReference(documentReference, entry);

    Extension repositoryUniqueIdExtension = documentReference
            .getExtensionByUrl(MagConstants.FhirExtensionUrls.REPOSITORY_UNIQUE_ID);
    if (repositoryUniqueIdExtension != null && repositoryUniqueIdExtension.getValue() instanceof Identifier) {
      Identifier identifier = (Identifier) repositoryUniqueIdExtension.getValue();
      entry.setRepositoryUniqueId(noPrefix(identifier.getValue()));
    }

    Extension documentAvailabilityExtension = documentReference
            .getExtensionByUrl(MagConstants.FhirExtensionUrls.DOCUMENT_AVAILABILITY);
    if (documentAvailabilityExtension != null && documentAvailabilityExtension.getValue() instanceof Coding) {
      Coding coding = (Coding) documentAvailabilityExtension.getValue();
      if (MagConstants.FhirCodingSystemIds.RFC_3986.equals(coding.getSystem()) && coding.getCode().startsWith("urn:ihe:iti:2010:DocumentAvailability:")) {
        entry.setDocumentAvailability(DocumentAvailability.valueOfOpcode(coding.getCode().substring(38)));
      }
    }

    submissionSet.setPatientId(entry.getPatientId());
    submissionSet.assignEntryUuid();
    builder.withDocument(entry);

    int version;
    Extension versionExtension = documentReference.getExtensionByUrl(MagConstants.FhirExtensionUrls.DOCUMENT_ENTRY_VERSION);
    if ((versionExtension != null) && (versionExtension.getValue() instanceof PositiveIntType)) {
      PositiveIntType versionElement = (PositiveIntType) versionExtension.getValue();
      version = versionElement.getValue();
    } else {
      version = 1;
    }

    builder.withAssociation(createHasMemberAssociationWithOriginalPreviousLabel(version, submissionSet, entry));

    // Submission contains a DocumentEntry object.
    // The logicalID attribute is present in the DocumentEntry object and has a UUID
    // formatted value.
    // The SubmissionSet to DocumentEntry HasMember Association has a Slot with name
    // PreviousVersion. This Slot has a single value, the version number of the
    // previous version, the one being replaced.

    return EbXML30Converters.convert(builder.build());
  }

  private Association createHasMemberAssociationWithOriginalPreviousLabel(int version, SubmissionSet submissionSet, DocumentEntry entry) {
    var assoc = createHasMemberAssociation(entry.getEntryUuid(), submissionSet);
    assoc.setLabel(AssociationLabel.ORIGINAL);
    assoc.setPreviousVersion(Integer.toString(version));
    assoc.setAssociationPropagation(true);
    return assoc;
  }

  private Association createHasMemberAssociation(String entryUuid, SubmissionSet submissionSet) {
    return new Association(AssociationType.HAS_MEMBER, new URN(UUID.randomUUID()).toString(),
        submissionSet.getEntryUuid(), entryUuid);

  }

}
