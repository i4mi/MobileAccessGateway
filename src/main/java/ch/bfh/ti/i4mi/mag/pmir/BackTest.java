package ch.bfh.ti.i4mi.mag.pmir;

import java.io.ByteArrayInputStream;

import javax.xml.bind.JAXBException;

import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02Type;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

public class BackTest {

	public void convert(byte[] response) throws JAXBException {
		System.out.println(new String(response));
		PRPAIN201310UV02Type msg = HL7V3Transformer.unmarshallMessage(PRPAIN201310UV02Type.class, new ByteArrayInputStream(response));
		System.out.println(msg.getId().toString());
	}
}
