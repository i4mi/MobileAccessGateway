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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ListIDPRouteBuilder extends RouteBuilder {

	@Override
	public void configure() throws Exception {								
		from("servlet://idps?httpMethodRestrict=GET").routeId("get-idps")
		.bean(ListIDPRouteBuilder.class, "listIDPs")		
	    .setHeader("Cache-Control", constant("no-store"))
	    .setHeader("Pragma", constant("no-cache"))
		.marshal().json();				
	}
	
	@Autowired
	@Qualifier("idps")
	Map<String, IDPConfig> idps;
	
	public JsonArray listIDPs() {
		JsonArray result = new JsonArray();
		
		for (Map.Entry<String, IDPConfig> idpDef : idps.entrySet()) {
		  JsonObject idp = new JsonObject();
		  idp.put("id", idpDef.getKey());
		  String name = idpDef.getValue().getName();
		  if (name == null) name = idpDef.getKey();
		  idp.put("name", name);
          result.add(idp);		  
		}
		
		return result;
	}
	
}
