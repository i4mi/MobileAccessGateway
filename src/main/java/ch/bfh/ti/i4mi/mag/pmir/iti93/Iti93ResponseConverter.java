package ch.bfh.ti.i4mi.mag.pmir.iti93;

import java.io.ByteArrayInputStream;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.MessageHeaderResponseComponent;
import org.hl7.fhir.r4.model.MessageHeader.MessageSourceComponent;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.openehealth.ipf.commons.ihe.fhir.translation.ToFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.mhd.Utils;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.mcciin000002UV01.MCCIIN000002UV01Type;
import net.ihe.gazelle.hl7v3.mccimt000200UV01.MCCIMT000200UV01AcknowledgementDetail;
import net.ihe.gazelle.hl7v3.prpain201310UV02.PRPAIN201310UV02MFMIMT700711UV01ControlActProcess;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

@Component
public class Iti93ResponseConverter implements ToFhirTranslator<byte[]> {

	@Autowired
	private Config config;
	
	public Bundle translateToFhir(byte[] input, Map<String, Object> parameters)  {
		try {
			
			System.out.println(new String(input));
			MCCIIN000002UV01Type  msg = HL7V3Transformer.unmarshallMessage(MCCIIN000002UV01Type.class, new ByteArrayInputStream(input));
									
			Bundle responseBundle = new Bundle()
                    .setType(Bundle.BundleType.MESSAGE);
                   				
			Bundle requestBundle = (Bundle) parameters.get(Utils.KEPT_BODY);
			MessageHeader header = (MessageHeader) requestBundle.getEntryFirstRep().getResource();
			
			MessageSourceComponent source = new MessageSourceComponent();
			source.setEndpoint(config.getBaseurl());
			header.setSource(source );
			
			MessageHeaderResponseComponent response = new MessageHeaderResponseComponent();
			
			response.setCode(ResponseType.OK);
			for (net.ihe.gazelle.hl7v3.mccimt000200UV01.MCCIMT000200UV01Acknowledgement akk : msg.getAcknowledgement()) {
				CS code = akk.getTypeCode();
				if (!code.getCode().equals("AA") && !code.getCode().equals("CA")) {
					response.setCode(ResponseType.FATALERROR);
					OperationOutcome outcome = new OperationOutcome();
					response.setDetails((Reference) new Reference().setResource(outcome));
					for (MCCIMT000200UV01AcknowledgementDetail detail : akk.getAcknowledgementDetail()) {
						OperationOutcomeIssueComponent issue = outcome.addIssue();
						issue.setDetails(new CodeableConcept().setText(detail.getText().toString()).addCoding(transform(detail.getCode())));
					     	
					}
				}
			}
			
			
			response.setIdentifier(header.getId());
			header.setId((String) null);
			header.setResponse(response );
			header.setFocus(null);
			
			BundleEntryComponent cmp = responseBundle.addEntry();
			cmp.setResource(header);			
			
			return responseBundle;
		} catch (JAXBException e) {
			throw new InvalidRequestException("failed parsing response");
		}
	}
	
	public Coding transform(CE code) {
		return new Coding(code.getCodeSystem(),code.getCode(),code.getDisplayName());
	}
}
