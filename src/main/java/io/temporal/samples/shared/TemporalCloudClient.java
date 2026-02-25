package io.temporal.samples.shared;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

/**
 * Shared utility for connecting to Temporal Cloud using API key authentication.
 *
 * <p>Requires the following environment variables:
 * <ul>
 *   <li>{@code TEMPORAL_ADDRESS} — e.g. {@code us-east-1.aws.api.temporal.io:7233}</li>
 *   <li>{@code TEMPORAL_NAMESPACE} — e.g. {@code my-namespace.acctid}</li>
 *   <li>{@code TEMPORAL_API_KEY} — the raw API key (no "Bearer " prefix)</li>
 * </ul>
 */
public final class TemporalCloudClient {

    private final WorkflowServiceStubs serviceStubs;
    private final WorkflowClient client;

    public TemporalCloudClient() {
        String address = requireEnv("TEMPORAL_ADDRESS");
        String namespace = requireEnv("TEMPORAL_NAMESPACE");
        String apiKey = requireEnv("TEMPORAL_API_KEY");

        this.serviceStubs =
                WorkflowServiceStubs.newServiceStubs(
                        WorkflowServiceStubsOptions.newBuilder()
                                .setTarget(address)
                                .setEnableHttps(true)
                                .addApiKey(() -> apiKey)
                                .build());

        this.client =
                WorkflowClient.newInstance(
                        serviceStubs,
                        WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }

    public WorkflowServiceStubs getServiceStubs() {
        return serviceStubs;
    }

    public WorkflowClient getClient() {
        return client;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: "
                            + name
                            + ". Set it before running (see .env file).");
        }
        return value;
    }
}
