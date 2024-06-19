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

import lombok.Data;

/**
 * Data used as response for OAuth2 token exchange 
 * @author alexander kreutz
 *
 */
@Data
public class OAuth2TokenResponse {

	private String access_token;
	
	private String refresh_token;
	
	private String token_type;
	
	private long expires_in;
	 
	private String scope;
		
}
