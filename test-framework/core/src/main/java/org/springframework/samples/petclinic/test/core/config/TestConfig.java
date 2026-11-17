package org.springframework.samples.petclinic.test.core.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@Sources({
    "system:properties",
    "system:env",
    "classpath:config.properties"
})
public interface TestConfig extends Config {

    @Key("gateway.url")
    @DefaultValue("http://localhost:8080")
    String gatewayUrl();

    @Key("db.url")
    @DefaultValue("jdbc:mysql://localhost:3306/petclinic?allowPublicKeyRetrieval=true&useSSL=false")
    String dbUrl();

    @Key("db.user")
    @DefaultValue("root")
    String dbUser();

    @Key("db.password")
    @DefaultValue("petclinic")
    String dbPassword();

    /** Time to open a connection to the gateway, in milliseconds. */
    @Key("http.connect.timeout.ms")
    @DefaultValue("5000")
    int httpConnectTimeoutMs();

    /** Time to wait for data from the gateway, in milliseconds. */
    @Key("http.read.timeout.ms")
    @DefaultValue("30000")
    int httpReadTimeoutMs();

}
