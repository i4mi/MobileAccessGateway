package ch.bfh.ti.i4mi.mag.mhd.iti65;

import org.hl7.fhir.r4.model.Organization;
import org.junit.jupiter.api.Test;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Hl7v2Based;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Iti65RequestConverter}.
 *
 * @author Quentin Ligier
 **/
class Iti65RequestConverterTest {

    @Test
    void testTransformOrganization() {
        final var iti65RequestConverter = new Iti65RequestConverter();

        final var org = new Organization();
        org.setName("Test");
        org.addIdentifier().setValue("1234");

        var xon = iti65RequestConverter.transform(org);
        assertEquals("Test^^^^^^^^^1234", Hl7v2Based.render(xon));

        org.getIdentifierFirstRep().setSystem("urn:oid:1.2.3");
        xon = iti65RequestConverter.transform(org);
        assertEquals("Test^^^^^&1.2.3&ISO^^^^1234", Hl7v2Based.render(xon));

        org.getIdentifierFirstRep().setValue("urn:oid:1.2.3").setSystem("urn:ietf:rfc:3986");
        xon = iti65RequestConverter.transform(org);
        assertEquals("Test^^^^^^^^^1.2.3", Hl7v2Based.render(xon));
    }
}