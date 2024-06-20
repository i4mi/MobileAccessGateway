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
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.context.SAMLMessageContext;

public class MySAMLContextProvider extends SAMLContextProviderLB {

    public MySAMLContextProvider(final String serverName, final String contextPath) {
        this.setScheme("https");
        this.setServerName(serverName);
        this.setContextPath(contextPath);
    }

    protected void populatePeerEntityId(final SAMLMessageContext context) throws MetadataProviderException {
        String localEntityId = context.getLocalEntityId();

        if (localEntityId != null && localEntityId.contains("/alias/")) {
            localEntityId = localEntityId.substring(localEntityId.lastIndexOf("/alias/") + 7);
        }
        String peerEntityId = this.metadata.getEntityIdForAlias(localEntityId + "idp");
        if (peerEntityId == null) {
            peerEntityId = this.metadata.getEntityIdForAlias(this.metadata.getDefaultIDP());
        }

        context.setPeerEntityId(peerEntityId);
        context.setPeerEntityRole(IDPSSODescriptor.DEFAULT_ELEMENT_NAME);
    }
}
