package io.temporal.samples.activities.namespacesetup;

import com.google.gson.JsonObject;
import io.temporal.activity.Activity;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.activities.helloworld.HelloWorldActivitiesImpl;
import io.temporal.samples.shared.TemporalCloudAdmin;
import io.temporal.samples.workflows.helloworld.HelloWorldWorkflow;
import io.temporal.samples.workflows.helloworld.HelloWorldWorkflowImpl;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class NamespaceSetupActivitiesImpl implements NamespaceSetupActivities {

    private static final String HELLO_TASK_QUEUE = "showcase-helloworld";
    private static final int MAX_POLL_ITERATIONS = 120;
    private static final long POLL_INTERVAL_MS = 5000;

    private final TemporalCloudAdmin admin;

    public NamespaceSetupActivitiesImpl(TemporalCloudAdmin admin) {
        this.admin = admin;
    }

    @Override
    public String createNamespace(String name, String region) {
        try {
            JsonObject result = admin.createNamespace(name, region, true);
            if (result.has("namespace")) {
                return result.get("namespace").getAsString();
            }
            return name;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create namespace: " + e.getMessage(), e);
        }
    }

    @Override
    public void waitForNamespaceActive(String namespaceId) {
        for (int i = 0; i < MAX_POLL_ITERATIONS; i++) {
            Activity.getExecutionContext().heartbeat("poll " + i);
            try {
                JsonObject result = admin.getNamespace(namespaceId);
                if (result.has("namespace") && result.getAsJsonObject("namespace").has("state")) {
                    String state = result.getAsJsonObject("namespace").get("state").getAsString();
                    if (state.contains("ACTIVE")) {
                        return;
                    }
                }
            } catch (IOException e) {
                if (e.getMessage() == null || !e.getMessage().contains("404")) {
                    throw new RuntimeException("Failed to get namespace state: " + e.getMessage(), e);
                }
                // 404 means namespace not yet visible — continue polling
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to get namespace state: " + e.getMessage(), e);
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for namespace", e);
            }
        }
        throw new RuntimeException(
                "Namespace " + namespaceId + " did not become ACTIVE within "
                        + (MAX_POLL_ITERATIONS * POLL_INTERVAL_MS / 1000) + " seconds");
    }

    @Override
    public String createServiceAccount(String name) {
        try {
            JsonObject result = admin.createServiceAccount(name, "Service account for " + name, "ROLE_READ");
            if (result.has("serviceAccountId")) {
                return result.get("serviceAccountId").getAsString();
            }
            throw new RuntimeException("No serviceAccountId in response: " + result);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create service account: " + e.getMessage(), e);
        }
    }

    @Override
    public void waitForServiceAccountReady(String serviceAccountId) {
        for (int i = 0; i < MAX_POLL_ITERATIONS; i++) {
            Activity.getExecutionContext().heartbeat("poll " + i);
            try {
                admin.getServiceAccount(serviceAccountId);
                return;
            } catch (IOException e) {
                if (e.getMessage() == null || !e.getMessage().contains("404")) {
                    throw new RuntimeException("Failed to check service account: " + e.getMessage(), e);
                }
                // 404 means not yet ready — continue polling
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to check service account: " + e.getMessage(), e);
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for service account", e);
            }
        }
        throw new RuntimeException(
                "Service account " + serviceAccountId + " did not become ready within "
                        + (MAX_POLL_ITERATIONS * POLL_INTERVAL_MS / 1000) + " seconds");
    }

    @Override
    public void grantNamespaceAccess(String namespaceId, String serviceAccountId) {
        try {
            admin.setServiceAccountNamespaceAccess(namespaceId, serviceAccountId, "PERMISSION_WRITE");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to grant namespace access: " + e.getMessage(), e);
        }
    }

    @Override
    public ApiKeyResult createApiKey(String displayName, String serviceAccountId) {
        try {
            String expiryTime = Instant.now().plus(90, ChronoUnit.DAYS).toString();
            JsonObject result = admin.createApiKey(
                    displayName, "API key for " + displayName,
                    "OWNER_TYPE_SERVICE_ACCOUNT", serviceAccountId, expiryTime);

            String keyId = result.has("keyId") ? result.get("keyId").getAsString() : null;
            String token = result.has("token") ? result.get("token").getAsString() : null;

            if (token != null) {
                System.out.println("!!! IMPORTANT: Save the API key token below — it will NOT be shown again !!!");
                System.out.println("    Token: " + token);
            }

            return new ApiKeyResult(keyId, token);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create API key: " + e.getMessage(), e);
        }
    }

    @Override
    public void waitForNamespaceConnectivity(String namespaceId, String region, String apiKeyToken) {
        String grpcEndpoint = regionToGrpcEndpoint(region);
        System.out.println("[waitForNamespaceConnectivity] endpoint=" + grpcEndpoint
                + " namespace=" + namespaceId
                + " apiKeyPrefix=" + apiKeyToken.substring(0, Math.min(20, apiKeyToken.length())) + "...");
        WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(grpcEndpoint)
                        .setEnableHttps(true)
                        .addApiKey(() -> apiKeyToken)
                        .build());
        try {
            WorkflowClient client = WorkflowClient.newInstance(
                    stubs,
                    WorkflowClientOptions.newBuilder().setNamespace(namespaceId).build());
            for (int i = 0; i < MAX_POLL_ITERATIONS; i++) {
                try {
                    // A simple gRPC call to verify connectivity and auth
                    client.getWorkflowServiceStubs().blockingStub()
                            .describeNamespace(
                                    io.temporal.api.workflowservice.v1.DescribeNamespaceRequest.newBuilder()
                                            .setNamespace(namespaceId)
                                            .build());
                    return;
                } catch (Exception e) {
                    System.out.println("[waitForNamespaceConnectivity] poll " + i
                            + " failed: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for namespace connectivity", e);
                }
                Activity.getExecutionContext().heartbeat("poll " + i);
            }
            throw new RuntimeException(
                    "Namespace " + namespaceId + " did not become reachable within "
                            + (MAX_POLL_ITERATIONS * POLL_INTERVAL_MS / 1000) + " seconds");
        } finally {
            stubs.shutdown();
        }
    }

    @Override
    public String runHelloWorldWorkflow(String namespaceId, String region, String apiKeyToken) {
        String grpcEndpoint = regionToGrpcEndpoint(region);
        System.out.println("Connecting to new namespace at: " + grpcEndpoint);

        WorkflowServiceStubs stubs = WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(grpcEndpoint)
                        .setEnableHttps(true)
                        .addApiKey(() -> apiKeyToken)
                        .build());

        WorkflowClient client = WorkflowClient.newInstance(
                stubs,
                WorkflowClientOptions.newBuilder().setNamespace(namespaceId).build());

        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(HELLO_TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());

        factory.start();
        System.out.println("HelloWorld worker started on new namespace, executing workflow...");

        try {
            HelloWorldWorkflow workflow = client.newWorkflowStub(
                    HelloWorldWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(HELLO_TASK_QUEUE)
                            .setWorkflowId("namespace-setup-helloworld-" + System.currentTimeMillis())
                            .build());

            String result = workflow.sayHello("Namespace Setup");
            System.out.println("HelloWorld result: " + result);
            return result;
        } finally {
            factory.shutdown();
            stubs.shutdown();
        }
    }

    /**
     * Converts a Temporal Cloud region (e.g. "aws-us-east-1") to a gRPC endpoint
     * (e.g. "us-east-1.aws.api.temporal.io:7233").
     */
    static String regionToGrpcEndpoint(String region) {
        // Region format: "<provider>-<region-part>" e.g. "aws-us-east-1"
        int firstDash = region.indexOf('-');
        if (firstDash < 0) {
            throw new IllegalArgumentException("Invalid region format: " + region
                    + ". Expected format: <provider>-<region> (e.g. aws-us-east-1)");
        }
        String provider = region.substring(0, firstDash);
        String regionPart = region.substring(firstDash + 1);
        return regionPart + "." + provider + ".api.temporal.io:7233";
    }
}
