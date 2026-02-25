package io.temporal.samples.activities.namespacesetup;

/** Result of API key creation. Includes the token so the workflow can pass it to the final activity. */
public record ApiKeyResult(String keyId, String token) {}
