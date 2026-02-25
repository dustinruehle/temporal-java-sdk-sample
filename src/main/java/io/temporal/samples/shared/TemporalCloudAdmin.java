package io.temporal.samples.shared;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client wrapper for the Temporal Cloud Operations API.
 *
 * <p>Uses the REST API at {@code https://saas-api.tmprl.cloud} to manage namespaces
 * programmatically. This is the required approach for Temporal Cloud — the Java SDK's {@code
 * RegisterNamespace} gRPC call only works with self-hosted Temporal.
 *
 * <p>Requires the {@code TEMPORAL_CLOUD_API_KEY} environment variable set to an admin-scoped API
 * key with namespace management permissions.
 */
public final class TemporalCloudAdmin {

    private static final String DEFAULT_BASE_URL = "https://saas-api.tmprl.cloud";
    private static final String API_VERSION = "2024-10-01-00";
    private static final String NAMESPACES_PATH = "/cloud/namespaces";

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public TemporalCloudAdmin() {
        this(requireEnv("TEMPORAL_CLOUD_API_KEY"), DEFAULT_BASE_URL);
    }

    public TemporalCloudAdmin(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        this.gson = new Gson();
    }

    /**
     * Creates a namespace on Temporal Cloud. Returns the parsed JSON response.
     *
     * @param name the namespace name (e.g. "my-namespace")
     * @param region the Cloud region ID (e.g. "aws-us-east-1")
     */
    public JsonObject createNamespace(String name, String region) throws IOException, InterruptedException {
        String requestBody = buildCreateRequestBody(name, region);

        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Create namespace");
    }

    /**
     * Deletes a namespace by its ID (the full name, e.g. {@code my-ns.acctid}).
     *
     * <p>Fetches the namespace first to obtain the current {@code resource_version}, which the
     * Cloud Operations API requires on delete to prevent race conditions.
     */
    public JsonObject deleteNamespace(String namespaceId) throws IOException, InterruptedException {
        // Fetch current resourceVersion — required by the API to prevent race conditions
        JsonObject ns = getNamespace(namespaceId);
        String resourceVersion = ns.getAsJsonObject("namespace").get("resourceVersion").getAsString();

        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH + "/" + namespaceId
                        + "?resource_version=" + resourceVersion)
                        .DELETE()
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Delete namespace");
    }

    /** Gets a namespace by its ID (the full name, e.g. {@code my-ns.acctid}). */
    public JsonObject getNamespace(String namespaceId) throws IOException, InterruptedException {
        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH + "/" + namespaceId)
                        .GET()
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Get namespace");
    }

    /** Lists all namespaces visible to the API key. */
    public JsonArray listNamespaces() throws IOException, InterruptedException {
        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH)
                        .GET()
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject result = handleResponse(response, "List namespaces");
        JsonElement namespaces = result.get("namespaces");
        return namespaces != null && namespaces.isJsonArray() ? namespaces.getAsJsonArray() : new JsonArray();
    }

    /**
     * Builds the JSON request body for namespace creation. Exposed for testing.
     *
     * @param name the namespace name
     * @param region the Cloud region ID (e.g. "aws-us-east-1")
     */
    public String buildCreateRequestBody(String name, String region) {
        JsonArray regions = new JsonArray();
        regions.add(region);

        JsonObject spec = new JsonObject();
        spec.addProperty("name", name);
        spec.add("regions", regions);
        spec.addProperty("retention_days", 30);

        JsonObject body = new JsonObject();
        body.add("spec", spec);

        return gson.toJson(body);
    }

    /** Parses a JSON response string into a JsonObject. Exposed for testing. */
    public JsonObject parseResponse(String json) {
        return gson.fromJson(json, JsonObject.class);
    }

    private HttpRequest.Builder newRequestBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + apiKey)
                .header("temporal-cloud-api-version", API_VERSION)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30));
    }

    private JsonObject handleResponse(HttpResponse<String> response, String operation)
            throws IOException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return gson.fromJson(response.body(), JsonObject.class);
        }
        throw new IOException(
                operation
                        + " failed (HTTP "
                        + status
                        + "): "
                        + response.body());
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
