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
import java.net.URLEncoder;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.commons.lang.RandomStringUtils;
import org.ehcache.Cache;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Convert SOAP assertion to OAuth2 response
 * @author alexander kreutz
 *
 */
public class AuthResponseConverter {

	@Autowired
	private Cache<String, AuthenticationRequest> codeToToken;	
	
	public String handle(@Body String assertion, @Header("oauthrequest") AuthenticationRequest request) throws UnsupportedEncodingException  {
				
		String returnurl = request.getRedirect_uri();
		String state = request.getState();		
		String code = RandomStringUtils.randomAlphanumeric(12);		
		request.setAssertion(assertion);
		
		codeToToken.put(code, request);
		
		if (returnurl.indexOf("?")<0) returnurl += "?"; else returnurl += "&";
		
		returnurl += "code="+URLEncoder.encode(code, "UTF-8");
		if (state != null) returnurl += "&state="+URLEncoder.encode(state, "UTF-8");
				
		return returnurl;
	}
	
	public String handleerror(@Header("oauthrequest") AuthenticationRequest request, @Body AuthException exception) throws UnsupportedEncodingException{
		System.out.println("CALLED ERROR HANDLER");
		String returnurl = request.getRedirect_uri();
		String state = request.getState();		
		
		if (returnurl.indexOf("?")<0) returnurl += "?"; else returnurl += "&";
		
		returnurl += "error="+URLEncoder.encode(exception.getError(), "UTF-8");
		returnurl += "&error_description="+URLEncoder.encode(exception.getMessage(), "UTF-8");
		if (state != null) returnurl += "&state="+URLEncoder.encode(state, "UTF-8");
		
		return returnurl;
	}
}
