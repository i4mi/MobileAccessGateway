package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import net.ihe.gazelle.hl7v3.mcciin000002UV01.MCCIIN000002UV01Type;

import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

public class Iti93ResponseConverter implements ToFhirTranslator<byte[]> {

	public Bundle translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			System.out.println(new String(input));
			MCCIIN000002UV01Type  msg = HL7V3Transformer.unmarshallMessage(MCCIIN000002UV01Type.class, new ByteArrayInputStream(input));
			
			Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.MESSAGE);
                    
			
			return responseBundle;
		} catch (JAXBException e) {
			throw new InvalidRequestException("failed parsing response");
		}
	}
}
