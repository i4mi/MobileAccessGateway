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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.bfh.ti.i4mi.mag.xua.ClientValidationService.ClientDefinition;

/**
 * Additional configuration for authentication services
 * @author alexander kreutz
 *
 */
@Configuration
public class AuthenticationConfig {

	private int TTL = 60;
	
	@Bean
	public Cache<String, AuthenticationRequest> getCodeToTokenCache() {
		 CacheManager cacheManager = 
				 CacheManagerBuilder
				     .newCacheManagerBuilder()
				     .withCache("codeToToken", 
				    		 CacheConfigurationBuilder
				    		    .newCacheConfigurationBuilder(String.class, AuthenticationRequest.class,
                                                              ResourcePoolsBuilder.heap(100))
				    		    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(TTL)))
				    		    .build())
				     .build(true);
		 return cacheManager.getCache("codeToToken", String.class, AuthenticationRequest.class);
	}
	
	@ConfigurationProperties("mag.iua.clients")
	@Bean(name = "clients")
	public Map<String, ClientDefinition> getClients() {
		return new HashMap<>();
	}
}
