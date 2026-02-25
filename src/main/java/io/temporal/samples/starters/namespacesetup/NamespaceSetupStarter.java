package io.temporal.samples.starters.namespacesetup;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.shared.TemporalCloudClient;
import io.temporal.samples.workflows.namespacesetup.NamespaceSetupInput;
import io.temporal.samples.workflows.namespacesetup.NamespaceSetupResult;
import io.temporal.samples.workflows.namespacesetup.NamespaceSetupWorkflow;

/**
 * Starts the namespace setup orchestration workflow.
 *
 * <p>Usage:
 * <pre>
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespacesetup.NamespaceSetupStarter \
 *   --args="my-test-ns aws-us-east-1"
 * </pre>
 */
public class NamespaceSetupStarter {

    private static final String TASK_QUEUE = "showcase-namespace-setup";
    private static final String DEFAULT_REGION = "aws-us-east-1";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: NamespaceSetupStarter <namespace-name> [region]");
            System.err.println("  region defaults to " + DEFAULT_REGION);
            System.exit(1);
        }

        String name = args[0];
        String region = args.length >= 2 ? args[1] : DEFAULT_REGION;

        TemporalCloudClient cloudClient = new TemporalCloudClient();
        WorkflowClient client = cloudClient.getClient();

        NamespaceSetupWorkflow workflow =
                client.newWorkflowStub(
                        NamespaceSetupWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("namespace-setup-" + name)
                                .build());

        System.out.println("Starting namespace setup workflow for: " + name + " in " + region);
        NamespaceSetupResult result = workflow.setup(new NamespaceSetupInput(name, region));

        System.out.println();
        System.out.println("=== Namespace Setup Complete ===");
        System.out.println("  Namespace:       " + result.namespaceId());
        System.out.println("  Service Account: " + result.serviceAccountId());
        System.out.println("  API Key ID:      " + result.apiKeyId());
        System.out.println("  HelloWorld:      " + result.helloWorldResult());

        System.exit(0);
    }
}
