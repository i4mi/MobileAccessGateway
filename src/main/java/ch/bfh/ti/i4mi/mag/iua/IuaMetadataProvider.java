package ch.bfh.ti.i4mi.mag.iua;

import ch.bfh.ti.i4mi.mag.Config;
import ch.bfh.ti.i4mi.mag.xua.IDPConfig;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MobileAccessGateway
 *
 * @author Quentin Ligier
 **/
@RestController
public class IuaMetadataProvider {

    private final Config springConfig;

    private final Map<String, IDPConfig> iuaIdps;

    public IuaMetadataProvider(final Config springConfig,
                               final @Qualifier("idps") Map<String, IDPConfig> iuaIdps) {
        this.springConfig = springConfig;
        this.iuaIdps = iuaIdps;
    }

    @GetMapping("/.well-known/oauth-authorization-server/{idp}")
    public AuthorizationServerMetadata provideAuthorizationMetadataForIdp(final @PathVariable String idp) {
        if (!this.iuaIdps.containsKey(idp)) {
            throw new IllegalArgumentException("Unknown IDP: " + idp);
        }
        final var idpConfig = this.iuaIdps.get(idp);
        final var metadata = new AuthorizationServerMetadata();
        // ...
        return metadata;
    }

    @Data
    public static class AuthorizationServerMetadata {
        private String issuer;
        private String authorization_endpoint;
        private String token_endpoint;
        private @Nullable String jwks_uri;
        private @Nullable List<String> scopes_supported;
        private List<String> response_types_supported;
        private List<String> grant_types_supported;
        private @Nullable List<String> token_endpoint_auth_methods_supported;
        private @Nullable String introspection_endpoint;
        private @Nullable List<String> introspection_endpoint_auth_methods_supported;
        private @Nullable List<String> access_token_format;
    }
}
