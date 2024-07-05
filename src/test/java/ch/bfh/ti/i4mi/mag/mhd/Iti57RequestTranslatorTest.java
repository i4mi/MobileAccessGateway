package ch.bfh.ti.i4mi.mag.mhd;

import ca.uhn.fhir.context.FhirContext;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.MagConstants;
import ch.bfh.ti.i4mi.mag.pmir.PatientReferenceCreator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ch.bfh.ti.i4mi.mag.mhd.iti67.Iti67RequestUpdateConverter;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.lcm.SubmitObjectsRequest;
import org.openehealth.ipf.platform.camel.ihe.xds.core.converters.XdsRenderingUtils;

public class Iti57RequestTranslatorTest {

    private static Iti67RequestUpdateConverter iti67RequestUpdateConverter;

    @BeforeAll
    public static void beforeAll() {
        Config config = new Config();
        config.setRepositoryUniqueId("1.1.4567332.1.2");
        config.setOidMpiPid("1.3.6.1.4.1.12559.11.20.1");
        SchemeMapper schemeMapper = new SchemeMapper();
        PatientReferenceCreator patientReferenceCreator = new PatientReferenceCreator();
        patientReferenceCreator.setConfig(config);
        patientReferenceCreator.setSchemeMapper(schemeMapper);
        iti67RequestUpdateConverter = new Iti67RequestUpdateConverter(config);
        iti67RequestUpdateConverter.setConfig(config);
        iti67RequestUpdateConverter.setSchemeMapper(schemeMapper);
        iti67RequestUpdateConverter.setPatientRefCreator(patientReferenceCreator);
    }

    @Test
    public void test1() {
        DocumentReference documentReference = (DocumentReference) FhirContext.forR4().newJsonParser().parseResource(Iti57RequestTranslatorTest.class.getClassLoader().getResourceAsStream("update-request-1.json"));
        SubmitObjectsRequest ebXml = iti67RequestUpdateConverter.convertDocumentReferenceToDocumentEntry(documentReference);
        String ebXmlString = XdsRenderingUtils.renderEbxml(ebXml);

        Assertions.assertTrue(ebXmlString.contains(MagConstants.XdsExtraMetadataSlotNames.CH_DELETION_STATUS));
        Assertions.assertTrue(ebXmlString.contains(MagConstants.XdsExtraMetadataSlotNames.CH_ORIGINAL_PROVIDER_ROLE));
        Assertions.assertTrue(ebXmlString.contains("repositoryUniqueId"));
        Assertions.assertTrue(ebXmlString.contains("documentAvailability"));
        Assertions.assertTrue(ebXmlString.contains("version"));
        Assertions.assertTrue(ebXmlString.contains("42"));
        Assertions.assertTrue(ebXmlString.contains("urn:uuid:18f08725-b77e-4d78-a409-b9f0ab4ef406"));

        Assertions.assertFalse(ebXmlString.contains("urn:oid:"));
    }

}
