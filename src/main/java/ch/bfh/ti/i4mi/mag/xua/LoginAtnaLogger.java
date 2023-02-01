package ch.bfh.ti.i4mi.mag.xua;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.event.UserAuthenticationBuilder;
import org.openehealth.ipf.commons.audit.types.PurposeOfUse;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginAtnaLogger {

	@Autowired
	private AuditContext myAuditContext;
	
	public void loginSuccess(@Header("oauthrequest") AuthenticationRequest request) {
		EventOutcomeIndicator outcome = EventOutcomeIndicator.Success;				
		atnaLog(outcome, null, PurposeOfUse.of("NORM", "2.16.756.5.30.1.127.3.10.5", "Normal Access"));
	}
	
	public void loginFail(@Header("oauthrequest") AuthenticationRequest request, @Body Exception fault) {
		EventOutcomeIndicator outcome = EventOutcomeIndicator.MinorFailure;				
		atnaLog(outcome, fault.toString(), PurposeOfUse.of("NORM", "2.16.756.5.30.1.127.3.10.5", "Normal Access"));
	}
	
	public void atnaLog(EventOutcomeIndicator outcome, String description, PurposeOfUse purposeOfUse) {
					
		myAuditContext.audit(
		new UserAuthenticationBuilder.Login(outcome, description, purposeOfUse)
		   .setAuditSource(myAuditContext)
		   
		   .getMessage()
		);
	}
	
}
