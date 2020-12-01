package ch.bfh.ti.i4mi.mag.audit;

import javax.annotation.PreDestroy;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.event.ApplicationActivityBuilder;
import org.openehealth.ipf.commons.audit.utils.AuditUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import ch.bfh.ti.i4mi.mag.MobileAccessGateway;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class Audit implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private AuditContext auditContext;
	
	@Value("${mag.name:MobileAccessGateway}")
	private String appName;
	
	public void start() {
		
	}
	
	@PreDestroy
	public void onStop() {
		log.info("Adding Application Stop Audit message");
		auditContext.audit(
			    new ApplicationActivityBuilder.ApplicationStop(EventOutcomeIndicator.Success)
			        .setAuditSource(auditContext)
			        .setApplicationParticipant(
			                appName,
			                null,
			                appName,
			                AuditUtils.getLocalHostName())
			        .addApplicationStarterParticipant(System.getProperty("user.name"))
			        .getMessage()
		);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		log.info("Adding Application Start Audit message");
		auditContext.audit(
			    new ApplicationActivityBuilder.ApplicationStart(EventOutcomeIndicator.Success)
			        .setAuditSource(auditContext)
			        .setApplicationParticipant(
			                appName,
			                null,
			                appName,
			                AuditUtils.getLocalHostName())
			        .addApplicationStarterParticipant(System.getProperty("user.name"))
			        .getMessage()
		);
	}
}
