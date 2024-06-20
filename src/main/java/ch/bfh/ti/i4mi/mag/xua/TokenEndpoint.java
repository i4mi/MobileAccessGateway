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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.apache.camel.Body;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Header;
import org.ehcache.Cache;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

/**
 * OAuth2 code to token exchange operation
 * @author alexander kreutz
 *
 */
@Slf4j
public class TokenEndpoint {
		
	@Autowired
	private Cache<String, AuthenticationRequest> codeToToken;
	
	@Autowired
	private ClientValidationService clients;
	
	@Value("${mag.iua.sp.disable-code-challenge:false}")
	private boolean disableCodeChallenge;

	/**
	 * The unmarshaller of Assertion elements.
	 */
	private final Unmarshaller assertionUnmarshaller;

	/**
	 * The SAML parser pool.
	 */
	private final BasicParserPool samlParserPool;

	public TokenEndpoint() {
		this.assertionUnmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(Assertion.DEFAULT_ELEMENT_NAME);
		this.samlParserPool = new BasicParserPool();
		this.samlParserPool.setNamespaceAware(true);
	}
	
	private void require(final String field, final String fieldname) throws AuthException {
		if (field == null || field.trim().isEmpty()) {
			throw new AuthException(400, "invalid_request", "'" + fieldname + "' is required");
		}
	}
	
	private void mustMatch(final String field, final String mustBe, final String fieldName) throws AuthException {
		if (!mustBe.equals(field)) {
			throw new AuthException(400, "invalid_request", "'"+fieldName+"' must be '"+mustBe+"'");
		}
	}
	
	public OAuth2TokenResponse handle(
			@Header("grant_type") String grantType, 
			@Header("code") String code,
			@Header("code_verifier") String codeVerifier,
			@Header("client_id") String clientId,
			@Header("client_secret") String clientSecret,
			@Header("redirect_uri") String redirectUri)
            throws UnsupportedEncodingException, AuthException, XMLParserException, UnmarshallingException {
		this.mustMatch(grantType, "authorization_code", "grant_type");
		this.require(code, "code");
		this.require(clientId, "client_id");
		this.require(redirectUri, "redirect_uri");
		if (!this.disableCodeChallenge) {
			this.require(codeVerifier, "code_verifier");
		}
	    
		final AuthenticationRequest request = this.codeToToken.get(code);
		
		if (request == null) {
			throw new AuthException(400, "access_denied", "Unknown code");
		}
		this.codeToToken.remove(code);
		
		mustMatch(clientId, request.getClient_id(), "client_id");
		mustMatch(redirectUri, request.getRedirect_uri(), "redirect_uri");
		
		if (!this.clients.isValidSecret(clientId, clientSecret)) {
			throw new AuthException(400, "access_denied", "Wrong client_secret");
		}
							
		if (!this.disableCodeChallenge && !sha256ThenBase64(codeVerifier).equals(request.getCode_challenge())) {
		  throw new AuthException(400, "access_denied", "Code challenge failed.");
		}
							
		final String assertion = request.getAssertion();
		log.debug("Assertion for token: {}", assertion);
		final String encoded = Base64.getEncoder().encodeToString(assertion.getBytes(StandardCharsets.UTF_8));
		log.debug("Encoded token: {}", encoded);

		String encodedIdp = Base64.getEncoder().encodeToString(request.getIdpAssertion().getBytes(StandardCharsets.UTF_8));
		
		final var result = new OAuth2TokenResponse();
		result.setAccess_token(encoded);
		result.setRefresh_token(encodedIdp);
		result.setExpires_in(this.computeExpiresInFromNotOnOrAfter(assertion)); // In seconds
		result.setScope(request.getScope());
		result.setToken_type("Bearer" /*request.getToken_type()*/);
		return result;
	}

	public static String sha256ThenBase64(String input) throws AuthException  {
		try {
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(input.getBytes(StandardCharsets.US_ASCII));
			return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new AuthException(400, "access_denied", "Code challenge failed.");
		}
    }
	
	public OAuth2TokenResponse generateOAuth2TokenResponse(final @ExchangeProperty("oauthrequest") AuthenticationRequest authRequest,
														   final @Body String assertion,
														   final @Header("scope") String scope) throws XMLParserException, UnmarshallingException {

		final String encoded = Base64.getEncoder().encodeToString(assertion.getBytes(StandardCharsets.UTF_8));
		final String idpAssertion = authRequest.getIdpAssertion();
		final String encodedIdp = Base64.getEncoder().encodeToString(idpAssertion.getBytes(StandardCharsets.UTF_8));
		
		final var result = new OAuth2TokenResponse();
		result.setAccess_token(encoded);
		result.setRefresh_token(encodedIdp);
		result.setExpires_in(this.computeExpiresInFromNotOnOrAfter(assertion)); // In seconds
		result.setScope(scope);
		result.setToken_type("Bearer" /*request.getToken_type()*/);
		return result;
	}
	
	public ErrorResponse handleError(final @Body AuthException in) {
		final var response = new ErrorResponse();
		response.setError(in.getError());
		response.setError_description(in.getMessage());
		return response;
	}

	/**
	 * Computes the number of seconds from now (inclusive) to the Assertion's @NotOnOrAfter attribute (exclusive).
	 *
	 * @param assertionXml The XML representation of the Assertion.
	 * @return a duration in seconds.
	 * @throws XMLParserException if the Assertion cannot be parsed.
	 * @throws UnmarshallingException if the Assertion cannot be unmarshalled.
	 */
	private long computeExpiresInFromNotOnOrAfter(final String assertionXml)
            throws XMLParserException, UnmarshallingException {
		// Parse the assertion and extract the NotOnOrAfter attribute
		final Element element = this.samlParserPool
				.parse(new ByteArrayInputStream(assertionXml.getBytes(StandardCharsets.UTF_8)))
				.getDocumentElement();
		final Assertion assertion = (Assertion) this.assertionUnmarshaller.unmarshall(element);

		final Instant notOnOrAfter = assertion.getConditions().getNotOnOrAfter().toDate().toInstant();
		return Duration.between(Instant.now(), notOnOrAfter).getSeconds();
	}
}
