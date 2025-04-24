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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.BadRequestException;

import org.apache.camel.Body;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convert OAuth2 request to SAML Assertion Request
 * @author alexander kreutz
 *
 */
@Component
public class AuthRequestConverter {

	public static final String SCOPE_PURPOSEOFUSE = "purpose_of_use=";
	public static final String RESOURCE_ID = "person_id=";
	public static final String ROLE = "subject_role=";
	public static final String PRINCIPAL_ID = "principal_id=";
	public static final String PRINCIPAL_NAME = "principal=";
	public static final String ORGANIZATION_ID = "group_id=";
	public static final String ORGANIZATION_NAME = "group=";
	
	@Autowired
	private ClientValidationService clients;
	
	@Value("${mag.iua.sp.disable-code-challenge:false}")
	private boolean disableCodeChallenge;

	@Autowired
	private OAuth2TokenEncryptionService tokenEncryptionService;
	
	public AuthenticationRequest buildAuthenticationRequest(
			final @Header("scope") String scope,
			final @Header("client_id") String clientId,
			final @Header("token_type") String tokenType,
			final @Header("redirect_uri") String redirect_uri,
			final @Header("state") String state,
			final @Header("code_challenge_method") String codeChallengeMethod,
			final @Header("code_challenge") String codeChallenge,
			final @Header("access_token_format") String accessTokenFormat) throws BadRequestException {
		if (redirect_uri == null) {
			throw new BadRequestException("redirect_uri is missing!");
		}
		if (!this.disableCodeChallenge) {
		  if (codeChallengeMethod == null || !codeChallengeMethod.equals("S256")) {
			  throw new BadRequestException("code_challenge_method must be 'S256'");
		  }
		  if (codeChallenge == null || codeChallenge.trim().isEmpty()) {
			  throw new BadRequestException("code_challenge is missing!");
		  }
		}
		
		if (tokenType != null && !tokenType.equals("Bearer")) {
			throw new BadRequestException("token_type must be 'Bearer'");
		}
		
		final var request = new AuthenticationRequest();
		request.setScope(scope);
		request.setRedirect_uri(redirect_uri);
		request.setClient_id(clientId);
		request.setState(state);
		request.setToken_type(accessTokenFormat);
		request.setCode_challenge(codeChallenge);
		return request;
	}
	
	public AssertionRequest buildAssertionRequest(final @Header("response_type") String responseType,
												  final @ExchangeProperty("oauthrequest") AuthenticationRequest request) throws AuthException {
		
		if (!"code".equals(responseType)) {
			throw this.throwInvalidRequest("response_type must be 'code'");
		}
		
		if (!this.clients.isValidClientId(request.getClient_id())) {
			throw this.throwInvalidRequest("Unknown client_id");
		}
		if (!this.clients.isValidRedirectUri(request.getClient_id(), request.getRedirect_uri())) {
			throw new BadRequestException("Invalid redirect uri");
		}
		
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			throw new AuthException(400, "access_denied", "authentication failed");
		}
		final Object authorization = auth.getPrincipal();
		return this.buildAssertionRequestInternal(authorization, request.getScope());
	}
	
	private String decode(String in) {
		return java.net.URLDecoder.decode(in, StandardCharsets.UTF_8);
	}
	
	public AssertionRequest buildAssertionRequestFromIdp(final @Body String authorization,
														 final @Header("scope") String scope) throws AuthException {
		return this.buildAssertionRequestInternal(authorization, scope);
	}
	
	public AssertionRequest buildAssertionRequestFromToken(final @Header("refresh_token") String refresh_token,
														   final @Header("scope") String scope) throws AuthException {
		try {
			final var idpAssertion = this.tokenEncryptionService.decrypt(refresh_token);
			return this.buildAssertionRequestInternal(idpAssertion, scope);
		} catch (final Exception e) {
			throw this.throwInvalidRequest("Invalid IDP assertion in OAuth2 token");
		}
    }
		
	private AssertionRequest buildAssertionRequestInternal(final Object authorization,
														   final String scope) throws AuthException {
		if (authorization == null) {
			throw this.throwInvalidRequest("missing IDP token");
		}
		if (scope == null || scope.isEmpty()) {
			throw this.throwInvalidRequest("missing scope parameter");
		}
		final var result = new AssertionRequest();
		for (final String scopePart : scope.split("\\s")) {
		  if (scopePart.startsWith(SCOPE_PURPOSEOFUSE)) {
			  result.setPurposeOfUse(token(scopePart.substring(SCOPE_PURPOSEOFUSE.length()),
										   "urn:oid:2.16.756.5.30.1.127.3.10.5"));
		  }
		  if (scopePart.startsWith(RESOURCE_ID)) {
			  result.setResourceId(scopePart.substring(RESOURCE_ID.length()));
		  }
          if (scopePart.startsWith(ROLE)) {
			  result.setRole(token(scopePart.substring(ROLE.length()), "urn:oid:2.16.756.5.30.1.127.3.10.6"));
		  }
          if (scopePart.startsWith(PRINCIPAL_ID)) {
			  result.setPrincipalID(scopePart.substring(PRINCIPAL_ID.length()));
		  }
          if (scopePart.startsWith(PRINCIPAL_NAME)) {
			  result.setPrincipalName(decode(scopePart.substring(PRINCIPAL_NAME.length())));
		  }
          if (scopePart.startsWith(ORGANIZATION_ID)) {
			  result.addOrganizationID(scopePart.substring(ORGANIZATION_ID.length()));
		  }
          if (scopePart.startsWith(ORGANIZATION_NAME)) {
			  result.addOrganizationName(decode(scopePart.substring(ORGANIZATION_NAME.length())));
		  }
		}
		result.setSamlToken(authorization);
		return result;
	}
	
	private String token(final String token, final String system) throws AuthException{
		if (token.startsWith(system + "|")) {
			return token.substring(system.length() + 1);
		}
		if (token.startsWith("|")) {
			return token.substring(1);
		}
		if (!token.contains("|")) {
			return token;
		}
		throw this.throwInvalidRequest("Invalid scope");
	}

	private AuthException throwInvalidRequest(final String message) {
		return new AuthException(400, "invalid_request", message);
	}
}
