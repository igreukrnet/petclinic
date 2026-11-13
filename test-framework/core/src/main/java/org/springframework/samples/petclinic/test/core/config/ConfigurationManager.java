package org.springframework.samples.petclinic.test.core.config;

import org.aeonbits.owner.ConfigFactory;

public final class ConfigurationManager {

    private static volatile TestConfig config;

    private ConfigurationManager() {}

    public static TestConfig getConfig() {
        if (config == null) {
            synchronized (ConfigurationManager.class) {
                if (config == null) {
                    config = ConfigFactory.create(TestConfig.class);
                }
            }
        }
        return config;
    }
}
