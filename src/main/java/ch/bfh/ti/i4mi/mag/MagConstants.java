package ch.bfh.ti.i4mi.mag;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MagConstants {

    @UtilityClass
    public static class FhirExtensionUrls {
        private static final String MHD_PREFIX = "https://profiles.ihe.net/ITI/MHD/StructureDefinition/";
        private static final String CH_PREFIX  = "http://fhir.ch/ig/ch-epr-fhir/";

        // standard MHD extensions
        public static final String REPOSITORY_UNIQUE_ID   = MHD_PREFIX + "ihe-repositoryUniqueId";
        public static final String DOCUMENT_ENTRY_VERSION = MHD_PREFIX + "ihe-version";
        public static final String DOCUMENT_AVAILABILITY  = MHD_PREFIX + "ihe-documentAvailability";

        // Swiss EPR specific extensions
        public static final String CH_AUTHOR_ROLE     = CH_PREFIX + "ch-ext-author-authorrole";
        public static final String CH_DELETION_STATUS = CH_PREFIX + "ch-ext-deletionstatus";
    }

    @UtilityClass
    public static class XdsExtraMetadataSlotNames {
        // Swiss EPR specific extra XDS metadata
        public static final String CH_ORIGINAL_PROVIDER_ROLE = "urn:e-health-suisse:2020:originalProviderRole";
        public static final String CH_DELETION_STATUS        = "urn:e-health-suisse:2019:deletionStatus";
    }

    // TODO: Move further string literals here
    @UtilityClass
    public static class FhirCodingSystemIds {
        public static final String RFC_3986 = "urn:ietf:rfc:3986";
        public static final String MHD_DOCUMENT_ID_TYPE = "https://profiles.ihe.net/ITI/MHD/CodeSystem/DocumentIdentifierTypes";
    }

    @UtilityClass
    public static class DeletionStatuses {
        private static final String PREFIX = "urn:e-health-suisse:2019:deletionStatus:";
        public static final String NOT_REQUESTED = PREFIX + "deletionNotRequested";
        public static final String REQUESTED     = PREFIX + "deletionRequested";
        public static final String PROHIBITED    = PREFIX + "deletionProhibited";
    }

}
