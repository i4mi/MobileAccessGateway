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

package ch.bfh.ti.i4mi.mag.mhd.pharm5;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Type;
import org.husky.communication.ch.camel.chpharm1.requests.ChQueryRegistry;
import org.husky.communication.ch.camel.chpharm1.requests.query.ChFindMedicationCardQuery;
import org.husky.communication.ch.camel.chpharm1.requests.query.ChFindMedicationListQuery;
import org.husky.communication.ch.camel.chpharm1.requests.query.ChPharmacyDocumentsQuery;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AssigningAuthority;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.AvailabilityStatus;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Code;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.DocumentEntryType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Identifiable;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.openehealth.ipf.commons.ihe.xds.core.requests.query.QueryReturnType;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.BaseRequestConverter;

/**
 * ITI-67 to PHARM-1 request converter
 * 
 * @author Oliver Egger
 *
 */
public class Pharm5RequestConverter extends BaseRequestConverter {

  public Timestamp timestampFromDate(Type date) {
    if (date == null)
      return null;
    String dateString = date.primitiveValue();
    if (dateString == null)
      return null;
    dateString = dateString.replaceAll("[T\\-:]", "");
    return Timestamp.fromHL7(dateString);
  }

  /**
   * convert PHARM-5 request to CMPD Pharm-1
   * 
   * @param searchParameter
   * @return
   */
  public ChQueryRegistry operationFindMedicationListToFindMedicationListQuery(@Body Parameters searchParameter) {

    boolean getLeafClass = true;

    ChPharmacyDocumentsQuery query = null;

    boolean chPmlQuery = true;
    // we query PML except if we are explicitly adding the formatcode for
    List<Type> formatTypes = searchParameter.getParameters(Pharm5Constants.PHARM5_FORMAT);
    if (formatTypes != null && formatTypes.size() > 0) {
      for (Type format : formatTypes) {
        Coding formatCoding = (Coding) format;
        if ("urn:ch:cda-ch-emed:medication-card:2018".equals(formatCoding.getCode())) {
          chPmlQuery = false;
        }
      }
    }
    if (chPmlQuery) {
      query = new ChFindMedicationListQuery();
    } else {
      query = new ChFindMedicationCardQuery();
    }

    // status --> $XDSDocumentEntryStatus
    List<Type> statusTypes = searchParameter.getParameters(Pharm5Constants.PHARM5_STATUS);
    if (statusTypes != null) {
      List<AvailabilityStatus> availabilites = new ArrayList<AvailabilityStatus>();
      for (Type status : statusTypes) {
        String tokenValue = status.primitiveValue();
        if (tokenValue.equals("current"))
          availabilites.add(AvailabilityStatus.APPROVED);
        else if (tokenValue.equals("superseded"))
          availabilites.add(AvailabilityStatus.DEPRECATED);
      }
      query.setStatus(availabilites);
    }

    // patient or patient.identifier --> $XDSDocumentEntryPatientId
    Type patientIdentifier = searchParameter.getParameter(Pharm5Constants.PHARM5_PATIENT_IDENTIFIER);
    if (patientIdentifier != null) {
      Identifier patIdentifier = (Identifier) patientIdentifier;
      String system = patIdentifier.getSystem();
      if (system == null || !system.startsWith("urn:oid:"))
        throw new InvalidRequestException("Missing OID for patient");
      query.setPatientId(new Identifiable(patIdentifier.getValue(), new AssigningAuthority(system.substring(8))));
    }

    // patient or patient.identifier --> $XDSDocumentEntryPatientId

    if (chPmlQuery) {
      Type serviceStartFrom = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_START_FROM);
      if (serviceStartFrom != null) {
        ((ChFindMedicationListQuery) query).getServiceStart().setFrom(timestampFromDate(serviceStartFrom));
      }
      Type serviceStartTo = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_START_TO);
      if (serviceStartTo != null) {
        ((ChFindMedicationListQuery) query).getServiceStart().setTo(timestampFromDate(serviceStartTo));
      }
      Type serviceEndFrom = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_END_FROM);
      if (serviceEndFrom != null) {
        ((ChFindMedicationListQuery) query).getServiceEnd().setFrom(timestampFromDate(serviceEndFrom));
      }
      Type serviceEndTo = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_END_TO);
      if (serviceEndTo != null) {
        ((ChFindMedicationListQuery) query).getServiceEnd().setTo(timestampFromDate(serviceEndTo));
      }
    } else {
      Type serviceStartFrom = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_START_FROM);
      if (serviceStartFrom != null) {
        ((ChFindMedicationCardQuery) query).getServiceStart().setFrom(timestampFromDate(serviceStartFrom));
      }
      Type serviceStartTo = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_START_TO);
      if (serviceStartTo != null) {
        ((ChFindMedicationCardQuery) query).getServiceStart().setTo(timestampFromDate(serviceStartTo));
      }
      Type serviceEndFrom = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_END_FROM);
      if (serviceEndFrom != null) {
        ((ChFindMedicationCardQuery) query).getServiceEnd().setFrom(timestampFromDate(serviceEndFrom));
      }
      Type serviceEndTo = searchParameter.getParameter(Pharm5Constants.PHARM5_SERVICE_END_TO);
      if (serviceEndTo != null) {
        ((ChFindMedicationCardQuery) query).getServiceEnd().setTo(timestampFromDate(serviceEndTo));
      }
    }

    formatTypes = searchParameter.getParameters(Pharm5Constants.PHARM5_FORMAT);
    if (formatTypes != null && formatTypes.size() > 0) {
      List<Code> formatCodes = new ArrayList<Code>();
      for (Type format : formatTypes) {
        Coding formatCoding = (Coding) format;
        Code formatCode = new Code();
        formatCode.setCode(formatCoding.getCode());
        String system = formatCoding.getSystem();
        if (system.startsWith("urn:oid:")) {
          system = system.substring(8);
        }
        formatCode.setSchemeName(system);
        formatCodes.add(formatCode);
      }
      query.setFormatCodes(formatCodes);
    }

    List<DocumentEntryType> documentEntryTypes = new ArrayList<DocumentEntryType>();
    documentEntryTypes.add(DocumentEntryType.ON_DEMAND);
    documentEntryTypes.add(DocumentEntryType.STABLE);
    
    if (chPmlQuery) {
      ((ChFindMedicationListQuery) query).setDocumentEntryTypes(documentEntryTypes);
    } else {
      ((ChFindMedicationCardQuery) query).setDocumentEntryTypes(documentEntryTypes);      
    }

    final ChQueryRegistry queryRegistry = new ChQueryRegistry(query);
    queryRegistry.setReturnType((getLeafClass) ? QueryReturnType.LEAF_CLASS : QueryReturnType.OBJECT_REF);

    return queryRegistry;
  }
}
