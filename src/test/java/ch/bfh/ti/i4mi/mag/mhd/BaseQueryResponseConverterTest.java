package ch.bfh.ti.i4mi.mag.mhd;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.iti67.Iti67ResponseConverter;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Hl7v2Based;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Organization;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BaseQueryResponseConverter}.
 *
 * @author Quentin Ligier
 **/
class BaseQueryResponseConverterTest {

    @Test
    void testTransformOrganization() {
        final var converter = new Iti67ResponseConverter(new Config());

        var fhirOrg = converter.transform(Hl7v2Based.parse("Test^^^^^^^^^1234", Organization.class));
        assertEquals("Test", fhirOrg.getName());
        assertEquals(1, fhirOrg.getIdentifier().size());
        assertEquals("1234", fhirOrg.getIdentifierFirstRep().getValue());
        assertFalse(fhirOrg.getIdentifierFirstRep().hasSystem());

        fhirOrg = converter.transform(Hl7v2Based.parse("Test^^^^^&1.2.3&ISO^^^^1234", Organization.class));
        assertEquals("Test", fhirOrg.getName());
        assertEquals(1, fhirOrg.getIdentifier().size());
        assertEquals("1234", fhirOrg.getIdentifierFirstRep().getValue());
        assertEquals("urn:oid:1.2.3", fhirOrg.getIdentifierFirstRep().getSystem());

        fhirOrg = converter.transform(Hl7v2Based.parse("Test^^^^^^^^^1.2.3", Organization.class));
        assertEquals("Test", fhirOrg.getName());
        assertEquals(1, fhirOrg.getIdentifier().size());
        assertEquals("urn:oid:1.2.3", fhirOrg.getIdentifierFirstRep().getValue());
        assertEquals("urn:ietf:rfc:3986", fhirOrg.getIdentifierFirstRep().getSystem());
    }
}