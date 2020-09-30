package ch.bfh.ti.i4mi.mag.xua;

import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.SAMLUtil;

public class IDPAssertionService implements SAMLUserDetailsService {

	@Override
	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
		try {
		   return XMLHelper.nodeToString(SAMLUtil.marshallMessage(credential.getAuthenticationAssertion()));
		} catch (MessageEncodingException e) {
		
		}
		return null;
	}

	
}
