package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.util.Collections;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCodeRole;
import org.openehealth.ipf.commons.audit.event.BaseAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.event.PatientRecordBuilder;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.openehealth.ipf.commons.audit.types.PurposeOfUse;
import org.openehealth.ipf.commons.ihe.core.atna.event.PHIImportBuilder;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirAuditStrategy;
import org.openehealth.ipf.commons.ihe.fhir.audit.codes.FhirEventTypeCode;
import org.openehealth.ipf.commons.ihe.fhir.support.OperationOutcomeOperations;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirAuditStrategy;
import org.openehealth.ipf.commons.ihe.fhir.support.OperationOutcomeOperations;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Iti93AuditStrategy extends FhirAuditStrategy<Iti93AuditDataset> {

    public Iti93AuditStrategy(boolean serverSide) {
        super(serverSide, OperationOutcomeOperations.INSTANCE);
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
        var documentManifest = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(DocumentManifest.class::isInstance)
                .map(DocumentManifest.class::cast)
                .findFirst().orElseThrow(() -> new RuntimeException("ITI-65 bundle must contain DocumentManifest"));

        
        //dataset.enrichDatasetFromDocumentManifest(documentManifest);
        return dataset;
    }

    @Override
    public boolean enrichAuditDatasetFromResponse(Iti93AuditDataset auditDataset, Object response, AuditContext auditContext) {
        var bundle = (Bundle) response;
        // Extract DocumentManifest (UU)IDs from the response bundle for auditing
        bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResponse)
                .filter(Objects::nonNull)
                .filter(r -> r.getLocation() != null && r.getLocation().startsWith("DocumentManifest"))
                .findFirst()
                .ifPresent(r -> auditDataset.setDocumentManifestUuid(r.getLocation()));
        return super.enrichAuditDatasetFromResponse(auditDataset, response, auditContext);
    }

    /**
     * Look at the response codes in the bundle entries and derive the ATNA event outcome
     * @param resource FHIR resource
     * @return RFC3881EventOutcomeCode
     */
    @Override
    protected EventOutcomeIndicator getEventOutcomeCodeFromResource(IBaseResource resource) {
        var bundle = (Bundle) resource;
        var responseStatus = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResponse)
                .map(Bundle.BundleEntryResponseComponent::getStatus)
                .collect(Collectors.toSet());

        if (responseStatus.stream().anyMatch(s -> s.startsWith("4") || s.startsWith("5"))) {
            return EventOutcomeIndicator.MajorFailure;
        }
        return EventOutcomeIndicator.Success;
    }
    
    @Override
    public AuditMessage[] makeAuditMessage(AuditContext auditContext, Iti93AuditDataset auditDataset) {
    	EventType eventType = EventType.of("ITI-93", "IHE Transactions", "Mobile Patient Identity Feed"); 
		EventActionCode action = EventActionCode.Execute;
		EventOutcomeIndicator outcome = auditDataset.getEventOutcomeIndicator();
		PurposeOfUse purposesOfUse = null;
		PatientRecordBuilder builder = new PatientRecordBuilder(outcome, action, eventType, purposesOfUse);
    	//builder.addSourceActiveParticipant(userId, altUserId, userName, networkId, isRequestor);
    	//builder.addDestinationActiveParticipant(userId, altUserId, userName, networkAccessPointId, userIsRequestor);
    	//builder.addParticipantObjectIdentification(objectIDTypeCode, objectName, objectQuery, objectDetails, objectID, objectTypeCode, objectTypeCodeRole, objectDataLifeCycle, objectSensitivity);
    	// Message identity
    	//builder.addParticipantObjectIdentification(objectIDTypeCode, objectName, objectQuery, objectDetails, objectID, objectTypeCode, objectTypeCodeRole, objectDataLifeCycle, objectSensitivity);
    	return builder.getMessages();
        /*return new PHIImportBuilder<>(auditContext, auditDataset, FhirEventTypeCode.ProvideDocumentBundle)
                .setPatient(auditDataset.getPatientId())
                .addImportedEntity(
                        auditDataset.getDocumentManifestUuid(),
                        ParticipantObjectIdTypeCode.XdsMetadata,
                        ParticipantObjectTypeCodeRole.Job,
                        Collections.emptyList())
                .getMessages();*/
    }
}
