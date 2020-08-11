package ch.bfh.ti.i4mi.mag.pmir.iti93;

import org.openehealth.ipf.commons.ihe.fhir.audit.FhirAuditDataset;

import lombok.Getter;
import lombok.Setter;

public class Iti93AuditDataset extends FhirAuditDataset {

    // Document manifest unique ID
    @Getter @Setter
    private String documentManifestUuid;

    public Iti93AuditDataset() {
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