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

package ch.bfh.ti.i4mi.mag.mhd.iti66;

import java.util.List;

import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ClassificationType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.IdentifiableType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryObjectListType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryPackageType;

/**
 * ITI-18 responses sometimes lack the type classification for the SubmissionSet. 
 * This causes the SubmissionSets to get lost during transformation from AdhocQueryResponse to QueryResponse
 * @author alexander kreutz
 *
 */
public class Iti66ResponseBugfix {

	/**
	 * bugfix: add missing type classification to SubmissionSets in ITI-18 response 
	 * @param response
	 * @return
	 */
	public AdhocQueryResponse fixResponse(@Body AdhocQueryResponse response) {
		RegistryObjectListType registryObjectList = response.getRegistryObjectList();
		if (registryObjectList != null) {
			var registryPackages = registryObjectList.getIdentifiable();
			if (registryPackages != null) {
				for (var possibleRegistryPackage : registryPackages) {

					IdentifiableType type = possibleRegistryPackage.getValue();
					if (type instanceof RegistryPackageType) {
						RegistryPackageType registryPackageType = (RegistryPackageType) type;
						boolean foundTypeClassification = false;
						for (var classification : registryPackageType.getClassification()) {
							if ("urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd".equals(classification.getClassificationNode())
								|| "urn:uuid:d9d542f3-6cc4-48b6-8870-ea235fbc94c2".equals(classification.getClassificationNode()))
								foundTypeClassification = true;
						}
						
						// If not found add SubmissionSet classification 
						if (!foundTypeClassification) {
							ClassificationType missing = new ClassificationType();
							missing.setClassificationNode("urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd");
							missing.setClassifiedObject(registryPackageType.getId());
							registryPackageType.getClassification().add(missing);
						}
					}

				}
			}
		}

		return response;
	}
}
