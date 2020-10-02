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

import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

/**
 * Convert OAuth2 request to SAML Assertion Request
 * @author alexander kreutz
 *
 */
public class AuthRequestConverter {

	public final static String SCOPE_PURPOSEOFUSE = "purposeOfUse/";
	public final static String RESOURCE_ID = "resourceId/";
	public final static String ROLE = "role/";
	public final static String PRINCIPAL_ID = "principalID/";
	public final static String ORGANIZATION = "organizationID/";
	
	public AuthenticationRequest buildAuthenticationRequest(
			@Header("scope") String scope, 			
			@Header("response_type") String responseType,
			@Header("client_id") String clientId,
			@Header("token_type") String tokenType,
			@Header("redirect_uri") String redirect_uri,
			@Header("state") String state,
			@Headers Map<String, Object> headers) {
		/*for (Map.Entry<String, Object> entry : headers.entrySet()) {
			System.out.println("HEADER: "+entry.getKey()+" = "+(entry.getValue()!=null?entry.getValue().toString():"null"));
		}*/
				
		if (!"code".equals(responseType)) throw new InvalidRequestException("response_type must be 'code'");
		
		if (tokenType==null) tokenType = "Bearer";
		
		AuthenticationRequest request = new AuthenticationRequest(); 
		request.setScope(scope);
		request.setRedirect_uri(redirect_uri);
		request.setClient_id(clientId);
		request.setState(state);
		request.setToken_type(tokenType);
						
		return request;
	}
	
	public AssertionRequest buildAssertionRequest(@Header("oauthrequest") AuthenticationRequest request) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) throw new InvalidRequestException("authentication failed");
		String authorization = auth.getPrincipal().toString();
						
		AssertionRequest result = new AssertionRequest();		
		String scopes[] = request.getScope().split("\\s");
		for (String scopePart : scopes) {
		  if (scopePart.startsWith(SCOPE_PURPOSEOFUSE)) result.setPurposeOfUse(scopePart.substring(SCOPE_PURPOSEOFUSE.length()));
		  if (scopePart.startsWith(RESOURCE_ID)) result.setResourceId(scopePart.substring(RESOURCE_ID.length()));
          if (scopePart.startsWith(ROLE)) result.setRole(scopePart.substring(ROLE.length()));
          if (scopePart.startsWith(PRINCIPAL_ID)) result.setPrincipalID(scopePart.substring(PRINCIPAL_ID.length()));
          if (scopePart.startsWith(ORGANIZATION)) result.setOrganizationID(scopePart.substring(ORGANIZATION.length()));
		}
				
		result.setSamlToken(authorization);
		System.out.println("Auth:"+result.getSamlToken());
		
		return result;
	}
}
