package ch.bfh.ti.i4mi.mag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "mag.iua")
public class IuaProperties {

    /**
     * The mapping of GenevaID patients email addresses (key) to EPR-SPIDs (value).
     */
    private Map<String, String> patients;

    public Map<String, String> getPatients() {
        return this.patients;
    }

    public void setPatients(final Map<String, String> patients) {
        this.patients = patients;
    }
}
