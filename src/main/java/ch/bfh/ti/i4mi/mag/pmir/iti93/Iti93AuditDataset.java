package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Identifier;
import org.openehealth.ipf.commons.ihe.fhir.audit.FhirAuditDataset;

import lombok.Getter;
import lombok.Setter;

public class Iti93AuditDataset extends FhirAuditDataset {

    // Document manifest unique ID
    @Getter @Setter
    private String eventUri;
    
    @Getter @Setter
    private String messageHeaderId;
    
    @Getter
    private List<Identifier> patients = new ArrayList<Identifier>();

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