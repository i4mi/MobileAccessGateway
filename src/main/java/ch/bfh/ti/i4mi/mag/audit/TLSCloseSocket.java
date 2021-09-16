package ch.bfh.ti.i4mi.mag.audit;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.AuditMetadataProvider;
import org.openehealth.ipf.commons.audit.TlsParameters;
import org.openehealth.ipf.commons.audit.protocol.TLSSyslogSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TLSCloseSocket extends TLSSyslogSenderImpl {

	private static final Logger log = LoggerFactory.getLogger(TLSCloseSocket.class);
	
	@Override
	public void send(AuditContext auditContext, AuditMetadataProvider auditMetadataProvider, String auditMessage) throws Exception {
		log.info("Auditing: "+auditMessage);		
		super.send(auditContext, auditMetadataProvider, auditMessage);
		shutdown();
	}

	public TLSCloseSocket(TlsParameters tlsParameters) {
		super(tlsParameters);
		// TODO Auto-generated constructor stub
	}

	
}
