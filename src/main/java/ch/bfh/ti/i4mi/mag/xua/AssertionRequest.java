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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * Information for a Get-X-User Assertion Request
 * @author alexander kreutz
 *
 */
public class AssertionRequest {
	
	@Getter @Setter
	private String purposeOfUse;
	
	@Getter @Setter
	private String role;

	/**
	 * When a patient is connecting from the Geneva PMP portal, the EPR-SPID is not specified in the request but will
	 * be determined by the user's email address and the configured mapping.
	 */
	@Getter @Setter @Nullable
	private String resourceId;
	
	@Getter @Setter
	private String principalID;
	
	@Getter @Setter
	private String principalName;
	
	@Getter @Setter
	private List<String> organizationID;
	
	@Getter @Setter
	private List<String> organizationName;
	
	@Getter @Setter
	private Object samlToken;
	
	public void addOrganizationID(String orgId) {
		if (organizationID==null) organizationID = new ArrayList<>();
		organizationID.add(orgId);
	}
	
	public void addOrganizationName(String orgName) {
		if (organizationName==null) organizationName = new ArrayList<>();
		organizationName.add(orgName);
	}
}
