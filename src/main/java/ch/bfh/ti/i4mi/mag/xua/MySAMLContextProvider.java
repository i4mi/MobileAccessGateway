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

import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLMessageContext;

public class MySAMLContextProvider extends SAMLContextProviderImpl {

	 protected void populatePeerEntityId(SAMLMessageContext context) throws MetadataProviderException {
		 
		 String localEntityId = context.getLocalEntityId();
		 
		 if (localEntityId != null && localEntityId.indexOf("/alias/")>=0) localEntityId = localEntityId.substring(localEntityId.lastIndexOf("/alias/")+7);
		 String peerEntityId = metadata.getEntityIdForAlias(localEntityId+"idp");
		 if (peerEntityId == null) peerEntityId = metadata.getEntityIdForAlias(metadata.getDefaultIDP());
		 
         context.setPeerEntityId(peerEntityId);		 
	     context.setPeerEntityRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
	 }
}
