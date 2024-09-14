package ch.bfh.ti.i4mi.mag;

import ch.bfh.ti.i4mi.mag.xua.IDPConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * The REST controller for the MobileAccessGateway. It provides some configuration for the GUI.
 *
 * @author Quentin Ligier
 **/
@RestController
public class MagRestController {

    private final Config springConfig;

    private final Map<String, IDPConfig> iuaIdps;

    public MagRestController(final Config springConfig,
                             final @Qualifier("idps") Map<String, IDPConfig> iuaIdps) {
        this.springConfig = springConfig;
        this.iuaIdps = iuaIdps;
    }

    @GetMapping("/mag-config")
    public MagConfig getMagConfig() {
        final var config = new MagConfig();
        config.setBaseUrl(this.springConfig.getBaseurl());
        config.setFhirBaseUrl(this.springConfig.getUriFhirEndpoint());

        for (final var iuaIdp : this.iuaIdps.values()) {
            config.getIua().put(iuaIdp.getName(), iuaIdp.getMetadataUrl());
        }

        return config;
    }

    @Data
    public static class MagConfig {
        private String baseUrl;
        private String fhirBaseUrl;
        private final Map<String, String> iua = new HashMap<>(4);
    }
}
