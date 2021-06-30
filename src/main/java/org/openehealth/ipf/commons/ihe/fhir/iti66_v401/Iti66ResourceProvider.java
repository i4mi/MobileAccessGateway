/*
 * Copyright 2016 the original author or authors.
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

package org.openehealth.ipf.commons.ihe.fhir.iti66_v401;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.openehealth.ipf.commons.ihe.fhir.AbstractPlainProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * Resource Provider for MHD (ITI-66)
 *
 * @author Alexander Kreutz, Christian Ohr
 * @since 3.6
 */
public class Iti66ResourceProvider extends AbstractPlainProvider {

    @SuppressWarnings("unused")
    @Search(type = ListResource.class)
    public IBundleProvider documentManifestSearch(
    		@OptionalParam(name = ListResource.SP_CODE) TokenOrListParam code,
            @RequiredParam(name = ListResource.SP_PATIENT, chainWhitelist = {"", Patient.SP_IDENTIFIER}) ReferenceParam patient,
            @OptionalParam(name = ListResource.SP_DATE) DateRangeParam date,
            @OptionalParam(name = ListResource.SP_SOURCE, chainWhitelist = { Practitioner.SP_FAMILY, Practitioner.SP_GIVEN }) ReferenceAndListParam author,
            @OptionalParam(name = ListResource.SP_IDENTIFIER) TokenParam identifier,
            @OptionalParam(name = "ListResource.designationType") TokenOrListParam designationType,
            @OptionalParam(name = "ListResource.sourceId") TokenOrListParam sourceId,
            @OptionalParam(name = ListResource.SP_SOURCE) ReferenceAndListParam source,
            @RequiredParam(name = ListResource.SP_STATUS) TokenOrListParam status,
            // Extension to ITI-66
            @OptionalParam(name = IAnyResource.SP_RES_ID) TokenParam resourceId,
            @Sort SortSpec sortSpec,
            @IncludeParam Set<Include> includeSpec,
            RequestDetails requestDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {


        var searchParameters = Iti66SearchParameters.builder()
                .date(date)
                .code(code)
                .sourceId(sourceId)
                .designationType(designationType)
                .status(status)
                .identifier(identifier)
                ._id(resourceId)
                .sortSpec(sortSpec)
                .includeSpec(includeSpec)
                .fhirContext(getFhirContext())
                .build();

        searchParameters.setSource(source);

        var chain = patient.getChain();
        if (Patient.SP_IDENTIFIER.equals(chain)) {
            searchParameters.setPatientIdentifier(patient.toTokenParam(getFhirContext()));
        } else if (chain == null || chain.isEmpty()) {
            searchParameters.setPatientReference(patient);
        }

        // Run down the route
        return requestBundleProvider(null, searchParameters, ResourceType.List.name(),
                httpServletRequest, httpServletResponse, requestDetails);
    }

    /**
     * Handles DocumentManifest Retrieve. This is not an actual part of the ITI-66 specification, but in the
     * context of restful FHIR IHE transaction it makes sense to be able to retrieve a DocumentManifest by
     * its resource ID.
     *
     * @param id resource ID
     * @param httpServletRequest servlet request
     * @param httpServletResponse servlet response
     * @param requestDetails      request details
     * @return {@link DocumentManifest} resource
     */
    @SuppressWarnings("unused")
    @Read(version = true, type = ListResource.class)
    public ListResource documentManifestRetrieve(
            @IdParam IdType id,
            RequestDetails requestDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        if (id == null) throw new InvalidRequestException("Must provide ID with READ request");
        // Run down the route
        return requestResource(id, null, ListResource.class, httpServletRequest, httpServletResponse, requestDetails);
    }

}
