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
import org.openehealth.ipf.commons.ihe.fhir.support.OperationOutcomeOperations;

/**
 * Audit strategy for ITI-104 transaction
 * @author alexander kreutz
 *
 */
public class Iti104AuditStrategy extends FhirAuditStrategy<Iti104AuditDataset> {

	//private String endpoint = "https://localhost:9091/fhir/$process-message";
	
    public Iti104AuditStrategy(boolean serverSide) {
        super(serverSide, OperationOutcomeOperations.INSTANCE);        
    }

    @Override
    public Iti104AuditDataset createAuditDataset() {
        return new Iti104AuditDataset();
    }

    @Override
    public Iti104AuditDataset enrichAuditDatasetFromRequest(Iti104AuditDataset auditDataset, Object request, Map<String, Object> parameters) {
        var dataset = super.enrichAuditDatasetFromRequest(auditDataset, request, parameters);
        var patient = (Patient) request;
        //
    
        //dataset.setEventUri(messageHeader.getEventUriType().asStringValue());
        //dataset.setMessageHeaderId(messageHeader.getId());        
        
        dataset.getPatients().add(patient.getIdentifierFirstRep());
                
        return dataset;
    }

    @Override
    public boolean enrichAuditDatasetFromResponse(Iti104AuditDataset auditDataset, Object response, AuditContext auditContext) {     
        return super.enrichAuditDatasetFromResponse(auditDataset, response, auditContext);
    }

    /**
     * Look at the response codes in the bundle entries and derive the ATNA event outcome
     * @param resource FHIR resource
     * @return RFC3881EventOutcomeCode
     */
    @Override
    protected EventOutcomeIndicator getEventOutcomeCodeFromResource(IBaseResource resource) {
    	if (! (resource instanceof Patient)) return super.getEventOutcomeCodeFromResource(resource);
    	return EventOutcomeIndicator.Success;                       
    }
    
    @Override
    public AuditMessage[] makeAuditMessage(AuditContext auditContext, Iti104AuditDataset auditDataset) {
    	EventType eventType = EventType.of("ITI-104", "IHE Transactions", "Mobile Patient Identity Feed"); 
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
