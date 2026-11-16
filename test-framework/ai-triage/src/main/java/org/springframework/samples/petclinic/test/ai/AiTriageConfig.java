package org.springframework.samples.petclinic.test.ai;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

/**
 * Settings for AI failure triage. Defaults live in {@code ai-triage.properties}; the usual system property
 * and environment variable overrides apply, for example {@code -Dai.triage.enabled=true}.
 */
@Sources({
    "system:properties",
    "system:env",
    "classpath:ai-triage.properties"
})
public interface AiTriageConfig extends Config {

    @Key("ai.triage.enabled")
    @DefaultValue("false")
    boolean aiTriageEnabled();

    @Key("ai.base.url")
    @DefaultValue("http://localhost:8045/v1")
    String aiBaseUrl();

    @Key("ai.api.key")
    @DefaultValue("antigravity")
    String aiApiKey();

    @Key("ai.model")
    @DefaultValue("gemini-3.8-flash")
    String aiModel();
}
