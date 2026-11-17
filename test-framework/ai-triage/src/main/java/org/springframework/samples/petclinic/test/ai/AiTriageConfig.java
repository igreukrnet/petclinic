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

    /** Base address of an OpenAI-compatible API, for example {@code https://host/v1}. No default. */
    @Key("ai.base.url")
    String aiBaseUrl();

    /** API key. No default, and never committed: pass it as {@code -Dai.api.key=...}. */
    @Key("ai.api.key")
    String aiApiKey();

    /** Model name as the endpoint expects it. No default. */
    @Key("ai.model")
    String aiModel();

    /** The most failures that are sent for analysis in one test run (one JVM). Later failures are skipped. */
    @Key("ai.triage.max.calls")
    @DefaultValue("5")
    int aiTriageMaxCalls();
}
