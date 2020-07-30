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

package ch.bfh.ti.i4mi.mag.pmir.iti83;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;

import ch.bfh.ti.i4mi.mag.BaseRequestConverter;
import ch.bfh.ti.i4mi.mag.Config;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.ST;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02QUQIMT021001UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201309UV02.PRPAIN201309UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02DataSource;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02ParameterList;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02PatientIdentifier;
import net.ihe.gazelle.hl7v3.prpamt201307UV02.PRPAMT201307UV02QueryByParameter;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * ITI-83 to ITI-45 request converter
 * @author alexander kreutz
 *
 */
public class Iti83RequestConverter extends BaseRequestConverter {

	  @Autowired
	  private Config config;
	
	  public String iti83ToIti45Converter(Parameters parameters) throws JAXBException  {
		  UriType targetSystem = (UriType) parameters.getParameter("targetSystem");
		  Identifier sourceIdentifier = (Identifier) parameters.getParameter("sourceIdentifier");
		  
		  PRPAIN201309UV02Type resultMsg = new PRPAIN201309UV02Type();
		  resultMsg.setITSVersion("XML_1.0");
		  //String UUID.randomUUID().toString();
		  resultMsg.setId(new II(config.getPixQueryOid(), uniqueId())); // "1.3.6.1.4.1.12559.11.1.2.2.5.7.1"
		  resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
		  resultMsg.setProcessingCode(new CS("T", null ,null));
		  resultMsg.setProcessingModeCode(new CS("T", null, null));
		  resultMsg.setInteractionId(new II("2.16.840.1.113883.1.18", "PRPA_IN201309UV02"));
		  resultMsg.setAcceptAckCode(new CS("AL", null, null));
		
		  MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
		  resultMsg.addReceiver(receiver);
		  receiver.setTypeCode(CommunicationFunctionType.RCV);
		  
		  MCCIMT000100UV01Device receiverDevice = new MCCIMT000100UV01Device();
		  receiver.setDevice(receiverDevice );
		  receiverDevice.setClassCode(EntityClassDevice.DEV);
		  receiverDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  receiverDevice.setId(Collections.singletonList(new II(config.getPixReceiverOid(), null)));
		  
		  MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
		  resultMsg.setSender(sender);
		  sender.setTypeCode(CommunicationFunctionType.SND);
		  
		  MCCIMT000100UV01Device senderDevice = new MCCIMT000100UV01Device();
		  sender.setDevice(senderDevice);
		  senderDevice.setClassCode(EntityClassDevice.DEV);
		  senderDevice.setDeterminerCode(EntityDeterminer.INSTANCE);
		  senderDevice.setId(Collections.singletonList(new II(config.getPixMySenderOid(), null)));
		 
		  PRPAIN201309UV02QUQIMT021001UV01ControlActProcess controlActProcess = new PRPAIN201309UV02QUQIMT021001UV01ControlActProcess();
		  resultMsg.setControlActProcess(controlActProcess);
		  controlActProcess.setClassCode(ActClassControlAct.CACT); // ???
		  controlActProcess.setMoodCode(XActMoodIntentEvent.EVN); // ???
		  controlActProcess.setCode(new CD("PRPA_TE201309UV02","2.16.840.1.113883.1.18", null)); // ???
		  
		  PRPAMT201307UV02QueryByParameter queryByParameter = new PRPAMT201307UV02QueryByParameter();
		  controlActProcess.setQueryByParameter(queryByParameter);
		  queryByParameter.setQueryId(new II(config.getPixQueryOid(), uniqueId()));//"1.3.6.1.4.1.12559.11.1.2.2.5.7.2"
		  queryByParameter.setStatusCode(new CS("new", null, null));
		  queryByParameter.setResponsePriorityCode(new CS("I", null, null));
		  
		  PRPAMT201307UV02ParameterList parameterList = new PRPAMT201307UV02ParameterList();
		  queryByParameter.setParameterList(parameterList);
		  
		  PRPAMT201307UV02PatientIdentifier patientIdentifier = new PRPAMT201307UV02PatientIdentifier();
		  parameterList.addPatientIdentifier(patientIdentifier);
		  String system = getScheme(sourceIdentifier.getSystem());
		  
		  patientIdentifier.setValue(Collections.singletonList(new II(system, sourceIdentifier.getValue())));		 
		  patientIdentifier.setSemanticsText(ST("Patient.id"));
		  
		  if (targetSystem != null) {
			String sourceSystem = getScheme(targetSystem.getValue());
			
			PRPAMT201307UV02DataSource dataSource = new PRPAMT201307UV02DataSource();
			parameterList.addDataSource(dataSource);
			dataSource.setValue(Collections.singletonList(new II(sourceSystem, null ,null)));			
			dataSource.setSemanticsText(ST("DataSource.id"));
		  }
		
		    ByteArrayOutputStream out = new ByteArrayOutputStream();
		    System.out.println("PRE CONVERT");
		    HL7V3Transformer.marshallMessage(PRPAIN201309UV02Type.class, out, resultMsg);
		    System.out.println("POST CONVERT");
		    String outArray = new String(out.toByteArray()); 
		    System.out.println(outArray);
		    return outArray;
		  
		  
	  }
}
