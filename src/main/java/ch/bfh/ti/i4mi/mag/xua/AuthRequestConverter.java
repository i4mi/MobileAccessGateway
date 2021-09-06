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

import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

/**
 * Convert OAuth2 request to SAML Assertion Request
 * @author alexander kreutz
 *
 */
@Component
public class AuthRequestConverter {

	public final static String SCOPE_PURPOSEOFUSE = "purpose_of_use=";
	public final static String RESOURCE_ID = "person_id=";
	public final static String ROLE = "subject_role=";
	public final static String PRINCIPAL_ID = "principal_id=";
	public final static String PRINCIPAL_NAME = "principa=";
	public final static String ORGANIZATION_ID = "group_id=";
	public final static String ORGANIZATION_NAME = "group=";
	
	@Autowired
	private ClientValidationService clients;
	
	@Value("${mag.iua.sp.disable-code-challenge:false}")
	private boolean disableCodeChallenge;
	
	public AuthenticationRequest buildAuthenticationRequest(
			@Header("scope") String scope, 						
			@Header("client_id") String clientId,
			@Header("token_type") String tokenType,
			@Header("redirect_uri") String redirect_uri,
			@Header("state") String state,
			@Header("code_challenge_method") String codeChallengeMethod,
			@Header("code_challenge") String codeChallenge,	
			@Header("access_token_format") String accessTokenFormat,
			@Headers Map<String, Object> headers) throws AuthException {
		/*for (Map.Entry<String, Object> entry : headers.entrySet()) {
			System.out.println("HEADER: "+entry.getKey()+" = "+(entry.getValue()!=null?entry.getValue().toString():"null"));
		}*/		
		if (redirect_uri == null) throw new BadRequestException("redirect_uri is missing!");
		if (!disableCodeChallenge) {
		  if (codeChallengeMethod==null || !codeChallengeMethod.equals("S256")) throw new BadRequestException("code_challenge_method must be 'S256'");
		  if (codeChallenge==null || codeChallenge.trim().length()==0) throw new BadRequestException("code_challenge is missing!");
		}
		
		if (tokenType!=null && !tokenType.equals("Bearer")) throw new BadRequestException("token_type must be Bearer");
		
		AuthenticationRequest request = new AuthenticationRequest(); 
		request.setScope(scope);
		request.setRedirect_uri(redirect_uri);
		request.setClient_id(clientId);
		request.setState(state);
		request.setToken_type(accessTokenFormat);
		request.setCode_challenge(codeChallenge);
						
		return request;
	}
	
	public AssertionRequest buildAssertionRequest(@Header("response_type") String responseType, @Header("oauthrequest") AuthenticationRequest request) throws AuthException {
		
		if (!"code".equals(responseType)) throw new AuthException(400, "invalid_request", "response_type must be 'code'");
		
		if (!clients.isValidClientId(request.getClient_id())) throw new AuthException(400, "invalid_request", "Unknown client_id");
		if (!clients.isValidRedirectUri(request.getClient_id(), request.getRedirect_uri())) throw new BadRequestException("Invalid redirect uri");
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) throw new AuthException(400, "access_denied", "authentication failed");
		String authorization = auth.getPrincipal().toString();
						
		return buildAssertionRequestFromIdp(authorization, request.getScope());		
	}
	
	public AssertionRequest buildAssertionRequestFromIdp(@Body String authorization, @Header("scope") String scope) throws AuthException {
        								
		if (authorization == null) throw new AuthException(400, "invalid_request", "missing IDP token");
		if (scope == null || scope.length()==0) throw new AuthException(400, "invalid_request", "missing scope parameter");
		AssertionRequest result = new AssertionRequest();		
		String scopes[] = scope.split("\\s");
		for (String scopePart : scopes) {
		  if (scopePart.startsWith(SCOPE_PURPOSEOFUSE)) result.setPurposeOfUse(token(scopePart.substring(SCOPE_PURPOSEOFUSE.length()),"urn:oid:2.16.756.5.30.1.127.3.10.5"));
		  if (scopePart.startsWith(RESOURCE_ID)) result.setResourceId(scopePart.substring(RESOURCE_ID.length()));
          if (scopePart.startsWith(ROLE)) result.setRole(token(scopePart.substring(ROLE.length()),"urn:oid:2.16.756.5.30.1.127.3.10.6"));
          if (scopePart.startsWith(PRINCIPAL_ID)) result.setPrincipalID(scopePart.substring(PRINCIPAL_ID.length()));
          if (scopePart.startsWith(PRINCIPAL_NAME)) result.setPrincipalName(scopePart.substring(PRINCIPAL_NAME.length()));
          if (scopePart.startsWith(ORGANIZATION_ID)) result.addOrganizationID(scopePart.substring(ORGANIZATION_ID.length()));
          if (scopePart.startsWith(ORGANIZATION_NAME)) result.addOrganizationName(scopePart.substring(ORGANIZATION_NAME.length()));
		}
				
		result.setSamlToken(authorization);
		System.out.println("Auth:"+result.getSamlToken());
		
		return result;
	}
	
	private String token(String token, String system) throws AuthException{
		if (token.startsWith(system+"|")) return token.substring(system.length()+1);
		if (token.startsWith("|")) return token.substring(1);
		if (token.indexOf("|")<0) return token;
		throw new AuthException(400,"invalid_request","Invalid scope");		
	}
}
