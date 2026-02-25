package io.temporal.samples.activities.namespacesetup;

import io.temporal.activity.ActivityInterface;

/**
 * Activities for namespace setup orchestration.
 *
 * <p>Each method maps to one Cloud Operations API call (or one gRPC interaction for the final
 * HelloWorld step). Keeping activities granular gives maximum visibility and retryability.
 */
@ActivityInterface
public interface NamespaceSetupActivities {

    /** Creates a namespace with API key auth enabled. Returns the full namespace ID. */
    String createNamespace(String name, String region);

    /** Polls until the namespace reaches ACTIVE state, heartbeating each iteration. */
    void waitForNamespaceActive(String namespaceId);

    /** Creates a service account with ROLE_READ. Returns the service account ID. */
    String createServiceAccount(String name);

    /** Polls until the service account is ready (GET succeeds), heartbeating each iteration. */
    void waitForServiceAccountReady(String serviceAccountId);

    /** Grants PERMISSION_WRITE access to the service account on the namespace. */
    void grantNamespaceAccess(String namespaceId, String serviceAccountId);

    /** Creates an API key for the service account. Returns key ID + token. */
    ApiKeyResult createApiKey(String displayName, String serviceAccountId);

    /** Polls until the API key can authenticate against the namespace via gRPC, heartbeating each iteration. */
    void waitForNamespaceConnectivity(String namespaceId, String region, String apiKeyToken);

    /** Connects to the new namespace, starts a HelloWorld worker, runs the workflow, returns the greeting. */
    String runHelloWorldWorkflow(String namespaceId, String region, String apiKeyToken);
}
