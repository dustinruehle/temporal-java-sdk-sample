package io.temporal.samples.workflows.namespacesetup;

/**
 * Result of the namespace setup workflow.
 *
 * <p>Note: The API key token is intentionally NOT stored here. It's sensitive and would be
 * persisted in workflow history. The activity logs it once for the user to capture.
 */
public record NamespaceSetupResult(
        String namespaceId,
        String serviceAccountId,
        String apiKeyId,
        String helloWorldResult) {}
