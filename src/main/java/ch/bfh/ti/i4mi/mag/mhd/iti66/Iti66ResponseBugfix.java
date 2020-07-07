package ch.bfh.ti.i4mi.mag.mhd.iti66;

import java.util.List;

import org.apache.camel.Body;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.query.AdhocQueryResponse;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.ClassificationType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.IdentifiableType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryObjectListType;
import org.openehealth.ipf.commons.ihe.xds.core.stub.ebrs30.rim.RegistryPackageType;

public class Iti66ResponseBugfix {

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
