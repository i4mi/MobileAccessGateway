package ch.bfh.ti.i4mi.mag.mhd.iti90;

import org.apache.camel.Body;
import org.hl7.fhir.r4.model.*;
import org.openehealth.ipf.commons.ihe.hpd.stub.dsmlv2.BatchResponse;
import org.openehealth.ipf.commons.ihe.hpd.stub.dsmlv2.DsmlAttr;
import org.openehealth.ipf.commons.ihe.hpd.stub.dsmlv2.SearchResponse;
import org.openehealth.ipf.commons.ihe.hpd.stub.dsmlv2.SearchResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.*;

/**
 * MobileAccessGateway
 *
 * @author Quentin Ligier
 **/
public class Iti90ResponseConverter {
    private static final Logger log = LoggerFactory.getLogger(Iti90ResponseConverter.class);

    final Bundle bundle = new Bundle();
    final Map<String, Organization> organizations = new HashMap<>();
    final Map<String, Practitioner> practitioners = new HashMap<>();
    final Map<String, PractitionerRole> practitionerRoles = new HashMap<>();

    public Resource convert(@Body final BatchResponse batchResponse) {
        final SearchResponse response = batchResponse.getBatchResponses().stream()
                .map(JAXBElement::getValue)
                .filter(SearchResponse.class::isInstance)
                .map(SearchResponse.class::cast)
                .findAny()
                .orElse(null);
        if (response == null) {
            final var oo = new OperationOutcome();
            oo.addIssue()
                    .setCode(OperationOutcome.IssueType.STRUCTURE)
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setDiagnostics("The response does not contain a 'searchResponse'");
            return oo;
        }

        final List<AttributeMap> hpdRegulatedOrganizations = new ArrayList<>();
        final List<AttributeMap> hpdProfessionals = new ArrayList<>();
        final List<AttributeMap> hpdRelationships = new ArrayList<>();

        this.bundle.setType(Bundle.BundleType.SEARCHSET);

        for (final SearchResultEntry entry : response.getSearchResultEntry()) {
            final AttributeMap attributes = new AttributeMap(entry.getAttr().size());
            for (final DsmlAttr attr : entry.getAttr()) {
                attributes.put(attr.getName(), (List<String>)(Object) attr.getValue());
            }

            if (!attributes.containsKey("objectClass")) {
                log.warn("HPD response does not contain objectClass");
                continue;
            }
            final List<String> objectClasses = attributes.get("objectClass");
            if (objectClasses.contains("HCRegulatedOrganization")) {
                hpdRegulatedOrganizations.add(attributes);
            } else if (objectClasses.contains("HCProfessional")) {
                hpdProfessionals.add(attributes);
            } else if (objectClasses.contains("groupOfNames")) {
                hpdRelationships.add(attributes);
            }
        }

        // We want to process the resources in this order, for reference creation reasons
        hpdRegulatedOrganizations.forEach(this::processRegulatedOrganization);
        hpdProfessionals.forEach(this::processProfessional);
        hpdRelationships.forEach(this::processRelationship);

        return this.bundle;
    }

    void processRegulatedOrganization(final AttributeMap attributes) {
        final var org = new Organization();

        // name
        if (attributes.containsKey("o")) {
            org.setName(attributes.get("o").get(0));
        }
        if (attributes.containsKey("hcRegisteredName")) {
            org.setName(attributes.getSingle("hcRegisteredName"));
        }

        // identifier
        if (attributes.containsKey("uid")) {

        }
        if (attributes.containsKey("hcIdentifier")) {

        }

        // active
        if (attributes.containsKey("hpdProviderStatus")) {
            org.setActive(!"inactive".equalsIgnoreCase(attributes.getSingle("hpdProviderStatus")));
        }

        // lastUpdated
        if (attributes.containsKey("modifyTimestamp")) {
            org.getMeta().setLastUpdatedElement(new InstantType(attributes.getSingle("modifyTimestamp")));
        }

        this.organizations.put("", org);
    }

    void processProfessional(final AttributeMap attributes) {

    }

    void processRelationship(final AttributeMap attributes) {

    }

    /**
     * A customized HashMap that lowercases the keys (because LDAP is case-insensitive) and provides methods to
     * retrieve the first element of the list value.
     */
    static class AttributeMap extends HashMap<String, List<String>> {

        public AttributeMap(final int initialCapacity) {
            super(initialCapacity);
        }

        public List<String> get(final String key) {
            return super.get(key.toLowerCase(Locale.ROOT));
        }

        public String getSingle(final String key) {
            return this.get(key).get(0);
        }

        public boolean containsKey(final String key) {
            return super.containsKey(key.toLowerCase(Locale.ROOT)) && !this.get(key).isEmpty();
        }

        @Override
        public List<String> put(final String key, final List<String> value) {
            return super.put(key.toLowerCase(Locale.ROOT), value);
        }
    }
}
