package ch.bfh.ti.i4mi.mag.fhir;

import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ch.bfh.ti.i4mi.mag.xua.Iti71RouteBuilder;
import ch.bfh.ti.i4mi.mag.xua.TokenEndpointRouteBuilder;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.openehealth.ipf.commons.ihe.fhir.support.NullsafeServerCapabilityStatementProvider;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.http.HttpServletRequest;

/**
 * A customized provider of the server CapabilityStatement that adds the OAuth URIs extension.
 *
 * @author Quentin Ligier
 */
@Interceptor
public class MagCapabilityStatementProvider extends NullsafeServerCapabilityStatementProvider {

    private final String baseUrl;

    public MagCapabilityStatementProvider(final RestfulServer fhirServer,
                                          @Value("${mag.baseurl}") final String baseUrl) {
        super(fhirServer);
        fhirServer.setServerConformanceProvider(this);
        this.baseUrl = baseUrl + "/camel/";
    }

    @Override
    public IBaseConformance getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        final var conformance = (CapabilityStatement) super.getServerConformance(theRequest, theRequestDetails);

        conformance.getRestFirstRep().getSecurity().addExtension()
                .setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris")
                .addExtension(new Extension("token",
                                            new StringType(this.baseUrl + TokenEndpointRouteBuilder.TOKEN_PATH)))
                .addExtension(new Extension("authorize",
                                            new StringType(this.baseUrl + Iti71RouteBuilder.AUTHORIZE_PATH)));

        return conformance;
    }
}
