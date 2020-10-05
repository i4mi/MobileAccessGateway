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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * OAuth 2 Client validator
 * Check validity of client_id, client_secret, redirect_uri
 * @author alexander kreutz
 *
 */
@Component
public class ClientValidationService {
	
	@Data
	public static class ClientDefinition {
		private String secret;
		
		private String redirect;
	}
	
	@Autowired
	@Qualifier(value = "clients")
	private Map<String, ClientDefinition> clients;
	
	
	
	public boolean isValidClientId(String clientId) {
		return clients.containsKey(clientId);
	}
	
	public boolean isValidSecret(String clientId, String clientSecret) {
		ClientDefinition def = clients.get(clientId);
		return def != null && def.getSecret().equals(clientSecret);
	}
	
	public boolean isValidRedirectUri(String clientId, String redirectUri) {
		ClientDefinition def = clients.get(clientId);
		return def != null && redirectUri.startsWith(def.getRedirect());
	}
}
