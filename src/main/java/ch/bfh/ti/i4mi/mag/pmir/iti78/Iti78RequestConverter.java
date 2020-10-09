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

package ch.bfh.ti.i4mi.mag.pmir.iti78;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.xml.bind.JAXBException;

import org.apache.camel.Header;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openehealth.ipf.commons.ihe.fhir.iti78.Iti78SearchParameters;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.pmir.PMIRRequestConverter;
import net.ihe.gazelle.hl7v3.datatypes.AD;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCity;
import net.ihe.gazelle.hl7v3.datatypes.AdxpCountry;
import net.ihe.gazelle.hl7v3.datatypes.AdxpPostalCode;
import net.ihe.gazelle.hl7v3.datatypes.AdxpState;
import net.ihe.gazelle.hl7v3.datatypes.CD;
import net.ihe.gazelle.hl7v3.datatypes.CE;
import net.ihe.gazelle.hl7v3.datatypes.CS;
import net.ihe.gazelle.hl7v3.datatypes.EN;
import net.ihe.gazelle.hl7v3.datatypes.EnFamily;
import net.ihe.gazelle.hl7v3.datatypes.EnGiven;
import net.ihe.gazelle.hl7v3.datatypes.II;
import net.ihe.gazelle.hl7v3.datatypes.IVLTS;
import net.ihe.gazelle.hl7v3.datatypes.IVXBTS;
import net.ihe.gazelle.hl7v3.datatypes.PN;
import net.ihe.gazelle.hl7v3.datatypes.TEL;
import net.ihe.gazelle.hl7v3.datatypes.TS;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Device;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Receiver;
import net.ihe.gazelle.hl7v3.mccimt000100UV01.MCCIMT000100UV01Sender;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02QUQIMT021001UV01ControlActProcess;
import net.ihe.gazelle.hl7v3.prpain201305UV02.PRPAIN201305UV02Type;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectAdministrativeGender;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectBirthTime;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectId;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02LivingSubjectName;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02MothersMaidenName;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02OtherIDsScopingOrganization;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02ParameterList;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02PatientAddress;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02PatientStatusCode;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02PatientTelecom;
import net.ihe.gazelle.hl7v3.prpamt201306UV02.PRPAMT201306UV02QueryByParameter;
import net.ihe.gazelle.hl7v3.voc.ActClassControlAct;
import net.ihe.gazelle.hl7v3.voc.CommunicationFunctionType;
import net.ihe.gazelle.hl7v3.voc.EntityClassDevice;
import net.ihe.gazelle.hl7v3.voc.EntityDeterminer;
import net.ihe.gazelle.hl7v3.voc.XActMoodIntentEvent;
import net.ihe.gazelle.hl7v3transformer.HL7V3Transformer;

/**
 * Convert ITI-78 to ITI-43 request
 * @author alexander kreutz
 *
 */
public class Iti78RequestConverter extends PMIRRequestConverter {

	  @Autowired
	  private Config config;
	
	  public IVXBTS transform(DateParam date) {
		   DateTimeType dt = new DateTimeType(date.getValueAsString());
		   return transform(dt);			
	  }
	  	 
	  public String iti78ToIti47Converter(@Header("FhirRequestParameters") Iti78SearchParameters parameters) throws JAXBException  {
					 		  
		  PRPAIN201305UV02Type resultMsg = new PRPAIN201305UV02Type();
		  resultMsg.setITSVersion("XML_1.0");
		  
		  resultMsg.setId(new II(config.getPixQueryOid(), uniqueId())); 
		  resultMsg.setCreationTime(new TS(Timestamp.now().toHL7())); // Now
		  resultMsg.setProcessingCode(new CS("T", null ,null));
		  resultMsg.setProcessingModeCode(new CS("T", null, null));
		  resultMsg.setInteractionId(new II("2.16.840.1.113883.1.6", "PRPA_IN201305UV02"));
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
		 		  
		  PRPAIN201305UV02QUQIMT021001UV01ControlActProcess controlActProcess = new PRPAIN201305UV02QUQIMT021001UV01ControlActProcess();
		  resultMsg.setControlActProcess(controlActProcess );
		  controlActProcess.setClassCode(ActClassControlAct.CACT); 
		  controlActProcess.setMoodCode(XActMoodIntentEvent.EVN);
		  controlActProcess.setCode(new CD("PRPA_TE201305UV02","2.16.840.1.113883.1.6", null)); 
		  		  
		  PRPAMT201306UV02QueryByParameter queryByParameter = new PRPAMT201306UV02QueryByParameter();
		  controlActProcess.setQueryByParameter(queryByParameter );
		  queryByParameter.setQueryId(new II(config.getPixQueryOid(), uniqueId()));
		  queryByParameter.setStatusCode(new CS("new", null, null));
		  queryByParameter.setResponsePriorityCode(new CS("I", null, null));
		  queryByParameter.setResponseModalityCode(new CS("R", null, null)); 
		  
		  PRPAMT201306UV02ParameterList parameterList = new PRPAMT201306UV02ParameterList();
		  queryByParameter.setParameterList(parameterList);
		  		  		  
		  TokenParam id = parameters.get_id();
		  if (id != null) {
			  String v = id.getValue();
			  int idx = v.indexOf("-");
			  if (idx > 0) {
				  PRPAMT201306UV02LivingSubjectId livingSubjectId = new PRPAMT201306UV02LivingSubjectId();
				  livingSubjectId.addValue(new II(v.substring(0,idx),v.substring(idx+1)));
				  livingSubjectId.setSemanticsText(ST("LivingSubject.id"));
				  parameterList.addLivingSubjectId(livingSubjectId);
			  }
		  }
		  
		  // active -> patientStatusCode
		  TokenParam active = parameters.getActive();
		  if (active != null) {
			  String activeCode = "active";
			  PRPAMT201306UV02PatientStatusCode patientStatusCode = new PRPAMT201306UV02PatientStatusCode();
			  patientStatusCode.setValue(new CS(activeCode,null,null));
			  parameterList.addPatientStatusCode(patientStatusCode );
		  }
		
		  // patientAddress
		  StringParam postalCode = parameters.getPostalCode();
		  StringParam state = parameters.getState();
		  StringParam city = parameters.getCity();
		  StringParam country = parameters.getCountry();		  
		  StringParam address = parameters.getAddress();
		  
		  if (postalCode != null || state != null || city !=null || country != null || address != null) {
			  PRPAMT201306UV02PatientAddress patientAddress = new PRPAMT201306UV02PatientAddress();
			  AD ad = new AD();
			  if (postalCode != null) ad.addPostalCode(element(AdxpPostalCode.class, postalCode.getValue()));
			  if (state != null) ad.addState(element(AdxpState.class, state.getValue()));
			  if (city != null) ad.addCity(element(AdxpCity.class, city.getValue()));
			  if (country != null) ad.addCountry(element(AdxpCountry.class, country.getValue()));
			  // TODO How to support address filter?
		      patientAddress.addValue(ad);
		      patientAddress.setSemanticsText(ST("Patient.addr"));
		      parameterList.addPatientAddress(patientAddress);
		  }
				 		  
		  // livingSubjectBirthTime
		  DateAndListParam birthdate = parameters.getBirthDate();
		  if (birthdate != null) {
			  for (DateOrListParam birthdateOr : birthdate.getValuesAsQueryTokens()) {
				  for (DateParam birthdateParam : birthdateOr.getValuesAsQueryTokens()) {
			
					  PRPAMT201306UV02LivingSubjectBirthTime livingSubjectBirthTime = new PRPAMT201306UV02LivingSubjectBirthTime();
					  IVLTS ivlts = new IVLTS();
					  
					  IVXBTS ivxbts = transform(birthdateParam);
					  switch(birthdateParam.getPrefix()) {
						case APPROXIMATE:
							break;
						case ENDS_BEFORE:
							ivlts.setHigh(ivxbts);
							break;
						case EQUAL:
							break;
						case GREATERTHAN:
							break;
						case GREATERTHAN_OR_EQUALS:
							break;
						case LESSTHAN:
							break;
						case LESSTHAN_OR_EQUALS:
							break;
						case NOT_EQUAL:
							break;
						case STARTS_AFTER:
							ivlts.setLow(ivxbts);
							break;
						default:
							break;					  
					  }
					  
					  livingSubjectBirthTime.addValue(ivlts);
					  livingSubjectBirthTime.setSemanticsText(ST("LivingSubject.birthTime"));
					  parameterList.addLivingSubjectBirthTime(livingSubjectBirthTime);
					  
				  }
			  }
		  }

		  // given, family -> livingSubjectName
		  StringAndListParam given = parameters.getGiven();
		  StringAndListParam family = parameters.getFamily();		  
		  StringParam givenElem = given != null ? given.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().get(0) : null;
		  StringParam familyElem = family != null ? family.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().get(0) : null;
		  
		  if (givenElem != null || familyElem != null) {
			  PRPAMT201306UV02LivingSubjectName livingSubjectName = new PRPAMT201306UV02LivingSubjectName();
			  livingSubjectName.setSemanticsText(ST("Patient.name"));
			  EN name = new EN();
			  
			  if (familyElem != null) {
				  name.addFamily(element(EnFamily.class, familyElem.getValue()));
				  if (!familyElem.isExact()) name.setUse("SRCH");
			  }
			  if (givenElem != null) {
				  name.addGiven(element(EnGiven.class, givenElem.getValue()));
				  if (!givenElem.isExact()) name.setUse("SRCH");
			  }
	          livingSubjectName.addValue(name);
	          livingSubjectName.setSemanticsText(ST("LivingSubject.name"));
			  parameterList.addLivingSubjectName(livingSubjectName );
		  }

	      // gender -> livingSubjectAdministrativeGender
		  TokenParam gender = parameters.getGender();
		  if (gender != null) {
			  PRPAMT201306UV02LivingSubjectAdministrativeGender livingSubjectAdministrativeGender = new PRPAMT201306UV02LivingSubjectAdministrativeGender();
			  switch (gender.getValue().toUpperCase()) {
			    case "MALE":livingSubjectAdministrativeGender.addValue(new CE("M","Male","2.16.840.1.113883.12.1"));break;
		        case "FEMALE":livingSubjectAdministrativeGender.addValue(new CE("F","Female","2.16.840.1.113883.12.1"));break;
		        case "OTHER":livingSubjectAdministrativeGender.addValue(new CE("A","Ambiguous","2.16.840.1.113883.12.1"));break;
		        case "UNKNOWN":livingSubjectAdministrativeGender.addValue(new CE("U","Unknown","2.16.840.1.113883.12.1"));break;
		        default: throw new InvalidRequestException("Unknown gender query parameter value");
			  }		  		  
		      parameterList.addLivingSubjectAdministrativeGender(livingSubjectAdministrativeGender );
		  }
		  
		  // identifiers -> livingSubjectId or otherIDsScopingOrganization
		  TokenAndListParam identifiers = parameters.getIdentifiers();		
		  if (identifiers != null) {
			  for (TokenOrListParam idOr : identifiers.getValuesAsQueryTokens()) {
				  for (TokenParam identifier : idOr.getValuesAsQueryTokens()) {
				
					  if (identifier.getValue() == null || identifier.getValue().length()==0) {
						  PRPAMT201306UV02OtherIDsScopingOrganization otherIDsScopingOrganization = new PRPAMT201306UV02OtherIDsScopingOrganization();
						  otherIDsScopingOrganization.addValue(new II(identifier.getSystem(), null));
						  otherIDsScopingOrganization.setSemanticsText(ST("OtherIDs.scopingOrganization.id"));
						  parameterList.addOtherIDsScopingOrganization(otherIDsScopingOrganization );					  
					  } else {
						  PRPAMT201306UV02LivingSubjectId livingSubjectId = new PRPAMT201306UV02LivingSubjectId();
						  livingSubjectId.addValue(new II(identifier.getSystem(),identifier.getValue()));
						  livingSubjectId.setSemanticsText(ST("LivingSubject.id"));
						  parameterList.addLivingSubjectId(livingSubjectId);
					  }
				  }
			  }		  
		  }
		  
		  // mothersMaidenName -> mothersMaidenName
		  StringParam mmn = parameters.getMothersMaidenName();
		  if (mmn != null) {
			  PRPAMT201306UV02MothersMaidenName mothersMaidenName = new PRPAMT201306UV02MothersMaidenName();
			  PN mothersMaidenNamePN = new PN();
			  mothersMaidenNamePN.addGiven(element(EnGiven.class, mmn.getValue()));
			  mothersMaidenName.addValue(mothersMaidenNamePN);
			  mothersMaidenName.setSemanticsText(ST("Person.MothersMaidenName"));
			  parameterList.addMothersMaidenName(mothersMaidenName );
		  }
		  
		  // telecom -> patientTelecom
		  StringParam telecom = parameters.getTelecom();
		  if (telecom != null) {
			  PRPAMT201306UV02PatientTelecom patientTelecom = new PRPAMT201306UV02PatientTelecom();
			  TEL tel = new TEL();
			  tel.setValue(telecom.getValue());
			  patientTelecom.addValue(tel);
			  parameterList.addPatientTelecom(patientTelecom );
		  }
		  
		  ByteArrayOutputStream out = new ByteArrayOutputStream();
		  System.out.println("PRE CONVERT");
		  HL7V3Transformer.marshallMessage(PRPAIN201305UV02Type.class, out, resultMsg);
		  System.out.println("POST CONVERT");
		  String outArray = new String(out.toByteArray()); 
		  System.out.println(outArray);
		  return outArray;
	     
	  }
	  
	  public String idConverter(@Header(value = "FhirHttpUri") String fhirHttpUri) throws JAXBException {
		   String uniqueId = fhirHttpUri.substring(fhirHttpUri.lastIndexOf("/") + 1);
		   Iti78SearchParameters params = Iti78SearchParameters.builder()._id(new TokenParam(uniqueId)).build();
		   return iti78ToIti47Converter(params);
	  }

}
