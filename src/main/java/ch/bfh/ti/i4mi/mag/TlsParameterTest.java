package ch.bfh.ti.i4mi.mag;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.camel.support.jsse.SSLContextParameters;
import org.openehealth.ipf.commons.audit.TlsParameters;

public class TlsParameterTest implements TlsParameters {

	private SSLContextParameters source;
	
	public TlsParameterTest(SSLContextParameters in) {
		source = in;
	}
	
	@Override
	public SSLContext getSSLContext() {
		try {
		  return source.createSSLContext(source.getCamelContext());
		} catch (Exception e) {}
		return null;
	}

}
