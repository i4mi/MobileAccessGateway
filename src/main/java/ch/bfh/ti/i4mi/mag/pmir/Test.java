package ch.bfh.ti.i4mi.mag.pmir;

import org.apache.camel.Body;

public class Test {

	public String getTest(@Body Object test) {
		
		return "<?xml version=\"1.0\" encoding=\"UTF8\" standalone=\"yes\"?>\n" + 
				"<PRPA_IN201309UV02 ITSVersion=\"XML_1.0\" xmlns=\"urn:hl7-org:v3\">\n" + 
				"    <id extension=\"1505\" root=\"1.3.6.1.4.1.12559.11.1.2.2.5.7.1\"/>\n" + 
				"    <creationTime value=\"20200702103549\"/>\n" + 
				"    <interactionId extension=\"PRPA_IN201309UV02\" root=\"2.16.840.1.113883.1.18\"/>\n" + 
				"    <processingCode code=\"T\"/>\n" + 
				"    <processingModeCode code=\"T\"/>\n" + 
				"    <acceptAckCode code=\"AL\"/>\n" + 
				"    <receiver typeCode=\"RCV\">\n" + 
				"        <device classCode=\"DEV\" determinerCode=\"INSTANCE\">\n" + 
				"            <id root=\"1.3.6.1.4.1.12559.11.1.2.2.5.11\"/>\n" + 
				"            <telecom value=\"https://gazelle.ihe.net/PAMSimulator-ejb/PIXManager_Service/PIXManager_PortType?wsdl\"/>\n" + 
				"        </device>\n" + 
				"    </receiver>\n" + 
				"    <sender typeCode=\"SND\">\n" + 
				"        <device classCode=\"DEV\" determinerCode=\"INSTANCE\">\n" + 
				"            <id root=\"1.3.6.1.4.1.12559.11.1.2.2.5.7\"/>\n" + 
				"        </device>\n" + 
				"    </sender>\n" + 
				"    <controlActProcess classCode=\"CACT\" moodCode=\"EVN\">\n" + 
				"        <code code=\"PRPA_TE201309UV02\" displayName=\"2.16.840.1.113883.1.18\"/>\n" + 
				"        <queryByParameter>\n" + 
				"            <queryId extension=\"1504\" root=\"1.3.6.1.4.1.12559.11.1.2.2.5.7.2\"/>\n" + 
				"            <statusCode code=\"new\"/>\n" + 
				"            <responsePriorityCode code=\"I\"/>\n" + 
				"            <parameterList>\n" + 
				"                <patientIdentifier>\n" + 
				"                    <value extension=\"IHEBLUE-3029\" root=\"1.3.6.1.4.1.21367.13.20.3000\"/>\n" + 
				"                    <semanticsText>Patient.id</semanticsText>\n" + 
				"                </patientIdentifier>\n" + 
				"            </parameterList>\n" + 
				"        </queryByParameter>\n" + 
				"    </controlActProcess>\n" + 
				"</PRPA_IN201309UV02>";
		
		
	}
}
