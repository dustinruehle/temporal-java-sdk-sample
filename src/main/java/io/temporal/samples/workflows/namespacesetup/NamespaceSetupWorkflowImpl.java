package io.temporal.samples.workflows.namespacesetup;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.samples.activities.namespacesetup.ApiKeyResult;
import io.temporal.samples.activities.namespacesetup.NamespaceSetupActivities;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class NamespaceSetupWorkflowImpl implements NamespaceSetupWorkflow {

    private final NamespaceSetupActivities activities =
            Workflow.newActivityStub(
                    NamespaceSetupActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(30))
                            .build());

    private final NamespaceSetupActivities pollingActivities =
            Workflow.newActivityStub(
                    NamespaceSetupActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(660))
                            .setHeartbeatTimeout(Duration.ofSeconds(30))
                            .build());

    private final NamespaceSetupActivities longRunningActivities =
            Workflow.newActivityStub(
                    NamespaceSetupActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(120))
                            .setRetryOptions(RetryOptions.newBuilder()
                                    .setMaximumAttempts(3)
                                    .build())
                            .build());

    @Override
    public NamespaceSetupResult setup(NamespaceSetupInput input) {
        // Step 1: Create namespace with API key auth
        String namespaceId = activities.createNamespace(input.name(), input.region());

        // Step 2: Wait until namespace is ACTIVE (activity polls internally with heartbeating)
        pollingActivities.waitForNamespaceActive(namespaceId);

        // Step 3: Create service account
        String serviceAccountId = activities.createServiceAccount("sa-" + input.name());

        // Step 4: Wait until service account is ready (activity polls internally with heartbeating)
        pollingActivities.waitForServiceAccountReady(serviceAccountId);

        // Step 5: Grant namespace access
        activities.grantNamespaceAccess(namespaceId, serviceAccountId);

        // Step 6: Create API key
        ApiKeyResult apiKeyResult = activities.createApiKey("key-" + input.name(), serviceAccountId);

        // Step 7: Wait until API key can authenticate against the new namespace
        pollingActivities.waitForNamespaceConnectivity(
                namespaceId, input.region(), apiKeyResult.token());

        // Step 8: Run HelloWorld workflow on the new namespace
        String greeting = longRunningActivities.runHelloWorldWorkflow(
                namespaceId, input.region(), apiKeyResult.token());

        return new NamespaceSetupResult(
                namespaceId, serviceAccountId, apiKeyResult.keyId(), greeting);
    }
}
