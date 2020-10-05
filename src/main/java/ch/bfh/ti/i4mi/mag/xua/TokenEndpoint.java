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


package ch.bfh.ti.i4mi.mag.xua;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.ehcache.Cache;
import org.jboss.resteasy.spi.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OAuth2 code to token exchange operation
 * @author alexander kreutz
 *
 */
public class TokenEndpoint {
		
	@Autowired
	private Cache<String, AuthenticationRequest> codeToToken;
	
	@Autowired
	private ClientValidationService clients;
	
	private long defaultTimeout = 1000l * 60l;
	
	private void require(String field, String fieldname) throws AuthException {
		if (field == null || field.trim().length() == 0) throw new AuthException(400, "invalid_request", "'"+fieldname+"' is required");
	}
	
	private void mustMatch(String field, String mustBe, String fieldName) throws AuthException {
		if (!mustBe.equals(field)) {			
			throw new AuthException(400, "invalid_request", "'"+fieldName+"' must be '"+mustBe+"'");
		}
	}
	
	public OAuth2TokenResponse handle(
			@Header("grant_type") String grantType, 
			@Header("code") String code,
			@Header("client_id") String clientId,
			@Header("client_secret") String clientSecret,
			@Header("redirect_uri") String redirectUri) throws UnsupportedEncodingException, AuthException {
				
		
		mustMatch(grantType, "authorization_code", "grant_type");
		require(code, "code");
		require(clientId, "client_id");
		require(redirectUri, "redirect_uri");
	    
		AuthenticationRequest request = codeToToken.get(code);
		
		if (request == null) throw new AuthException(400, "access_denied", "Unknown code");
		codeToToken.remove(code);
		
		mustMatch(clientId, request.getClient_id(), "client_id");
		mustMatch(redirectUri, request.getRedirect_uri(), "redirect_uri");
		
		if (!clients.isValidSecret(clientId, clientSecret)) throw new AuthException(400, "access_denied", "Wrong client_secret");
		
		String assertion = request.getAssertion();
		String encoded = Base64.getEncoder().encodeToString(assertion.getBytes("UTF-8"));
		//String token = "IHE-SAML "+encoded;
		
		OAuth2TokenResponse result = new OAuth2TokenResponse();
		result.setAccess_token(encoded);
		result.setExpires_in(defaultTimeout);
		result.setScope(request.getScope());
		result.setToken_type("IHE-SAML" /*request.getToken_type()*/);
		return result;
				
	}
	
	public ErrorResponse handleError(@Body AuthException in) {
		ErrorResponse response = new ErrorResponse();
		response.setError(in.getError());
		response.setError_description(in.getMessage());
		return response;
	}

}
