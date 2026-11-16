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

}
