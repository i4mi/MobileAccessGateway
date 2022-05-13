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

package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.Patient;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectDataLifeCycle;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCodeRole;
import org.openehealth.ipf.commons.audit.event.PatientRecordBuilder;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.openehealth.ipf.commons.audit.types.PurposeOfUse;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirAuditStrategy;

/**
 * Audit strategy for ITI-93 transaction
 * @author alexander kreutz
 *
 */
public class Iti93AuditStrategy extends FhirAuditStrategy<Iti93AuditDataset> {

	//private String endpoint = "https://localhost:9091/fhir/$process-message";
	
    public Iti93AuditStrategy(boolean serverSide) {
        super(serverSide);
    }

    @Override
    public Iti93AuditDataset createAuditDataset() {
        return new Iti93AuditDataset();
    }

    @Override
    public Iti93AuditDataset enrichAuditDatasetFromRequest(Iti93AuditDataset auditDataset, Object request, Map<String, Object> parameters) {
        var dataset = super.enrichAuditDatasetFromRequest(auditDataset, request, parameters);
        var bundle = (Bundle) request;
        //
        var messageHeader = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(MessageHeader.class::isInstance)
                .map(MessageHeader.class::cast)
                .findFirst().orElseThrow(() -> new RuntimeException("ITI-93 bundle must contain MessageHeader"));

        dataset.setEventUri(messageHeader.getEventUriType().asStringValue());
        dataset.setMessageHeaderId(messageHeader.getId());
        
        bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(Patient.class::isInstance)
        .map(Patient.class::cast)
        .forEach(patient -> { 
        	dataset.getPatients().add(patient.getIdentifierFirstRep());
         });
        
        return dataset;
    }

    @Override
    public boolean enrichAuditDatasetFromResponse(Iti93AuditDataset auditDataset, Object response, AuditContext auditContext) {     
        return super.enrichAuditDatasetFromResponse(auditDataset, response, auditContext);
    }

    /**
     * Look at the response codes in the bundle entries and derive the ATNA event outcome
     * @param resource FHIR resource
     * @return RFC3881EventOutcomeCode
     */
    @Override
    protected EventOutcomeIndicator getEventOutcomeCodeFromResource(Iti93AuditDataset auditDataset, IBaseResource resource) {
    	if (! (resource instanceof Bundle)) return super.getEventOutcomeCodeFromResource(auditDataset, resource);
        var bundle = (Bundle) resource;
        
        var messageHeader = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(MessageHeader.class::isInstance)
                .map(MessageHeader.class::cast)
                .findFirst().orElseThrow(() -> new RuntimeException("ITI-93 bundle must contain MessageHeader"));

        ResponseType result = messageHeader.getResponse().getCode();
        if (result == null) return EventOutcomeIndicator.MajorFailure;
        if (result.equals(ResponseType.OK)) return EventOutcomeIndicator.Success;
        if (result.equals(ResponseType.TRANSIENTERROR)) return EventOutcomeIndicator.MinorFailure;
        return EventOutcomeIndicator.MajorFailure;
        
    }
    
    @Override
    public AuditMessage[] makeAuditMessage(AuditContext auditContext, Iti93AuditDataset auditDataset) {
    	EventType eventType = EventType.of("ITI-93", "IHE Transactions", "Mobile Patient Identity Feed"); 
		EventActionCode action = EventActionCode.Execute;
		EventOutcomeIndicator outcome = auditDataset.getEventOutcomeIndicator();
		PurposeOfUse purposesOfUse = null;
		PatientRecordBuilder builder = new PatientRecordBuilder(outcome, action, eventType, purposesOfUse);
		builder.setAuditSource(auditContext);
    	builder.addSourceActiveParticipant(auditDataset.getSourceUserId(), null, auditDataset.getSourceUserName(), auditDataset.getRemoteAddress(), true);
    	builder.addDestinationActiveParticipant(auditDataset.getDestinationUserId(), null, null, auditDataset.getLocalAddress(), false);
    	
		for (Identifier id : auditDataset.getPatients()) {
			builder.addPatientParticipantObject(id.getValue(), null, null, null);	
		}
				
		builder.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.NodeID,
                auditDataset.getEventUri(),
                null,
                null,
                auditDataset.getMessageHeaderId(),
                ParticipantObjectTypeCode.Other,
                ParticipantObjectTypeCodeRole.Resource,
                ParticipantObjectDataLifeCycle.Origination,
                null);
		    	
    	return builder.getMessages();      
    }
}
