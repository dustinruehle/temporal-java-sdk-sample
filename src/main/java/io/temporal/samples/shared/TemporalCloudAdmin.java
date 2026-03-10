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
import java.util.Map;

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
    private static final String API_KEYS_PATH = "/cloud/api-keys";
    private static final String SERVICE_ACCOUNTS_PATH = "/cloud/service-accounts";

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
        return createNamespace(name, region, false);
    }

    /**
     * Creates a namespace on Temporal Cloud, optionally enabling API key authentication.
     *
     * @param name the namespace name (e.g. "my-namespace")
     * @param region the Cloud region ID (e.g. "aws-us-east-1")
     * @param enableApiKeyAuth whether to enable API key auth on the new namespace
     */
    public JsonObject createNamespace(String name, String region, boolean enableApiKeyAuth)
            throws IOException, InterruptedException {
        String requestBody = buildCreateRequestBody(name, region, enableApiKeyAuth);

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
     * Updates an existing namespace on Temporal Cloud. Fetches the current namespace to obtain
     * the {@code resourceVersion} and current spec, merges in the provided spec updates, and
     * POSTs the update.
     *
     * @param namespaceId the full namespace ID (e.g. "my-ns.acctid")
     * @param specUpdates a JsonObject containing the spec fields to merge/override
     */
    public JsonObject updateNamespace(String namespaceId, JsonObject specUpdates)
            throws IOException, InterruptedException {
        JsonObject ns = getNamespace(namespaceId);
        JsonObject nsObj = ns.getAsJsonObject("namespace");
        String resourceVersion = nsObj.get("resourceVersion").getAsString();

        // Start with the current spec and merge updates
        JsonObject currentSpec = nsObj.has("spec") ? nsObj.getAsJsonObject("spec").deepCopy() : new JsonObject();
        for (String key : specUpdates.keySet()) {
            currentSpec.add(key, specUpdates.get(key));
        }

        String requestBody = buildUpdateRequestBody(currentSpec, resourceVersion);

        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH + "/" + namespaceId)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Update namespace");
    }

    /**
     * Creates an API key on Temporal Cloud for a service account.
     *
     * @param displayName the display name for the API key
     * @param description optional description (may be null)
     * @param ownerType the owner type (e.g. "OWNER_TYPE_SERVICE_ACCOUNT")
     * @param ownerId the service account ID
     * @param expiryTime optional RFC 3339 expiry time (may be null)
     */
    public JsonObject createApiKey(String displayName, String description,
            String ownerType, String ownerId, String expiryTime)
            throws IOException, InterruptedException {
        String requestBody = buildCreateApiKeyRequestBody(
                displayName, description, ownerType, ownerId, expiryTime);

        HttpRequest request =
                newRequestBuilder(API_KEYS_PATH)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Create API key");
    }

    /**
     * Creates a service account on Temporal Cloud.
     *
     * @param name display name for the service account
     * @param description optional description (may be null)
     * @param accountRole the account-level role (e.g. "ROLE_READ", "ROLE_DEVELOPER")
     */
    public JsonObject createServiceAccount(String name, String description, String accountRole)
            throws IOException, InterruptedException {
        String requestBody = buildCreateServiceAccountRequestBody(name, description, accountRole);

        HttpRequest request =
                newRequestBuilder(SERVICE_ACCOUNTS_PATH)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Create service account");
    }

    /** Gets a service account by its ID (e.g. {@code sa-xxxx}). */
    public JsonObject getServiceAccount(String serviceAccountId)
            throws IOException, InterruptedException {
        HttpRequest request =
                newRequestBuilder(SERVICE_ACCOUNTS_PATH + "/" + serviceAccountId)
                        .GET()
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Get service account");
    }

    /**
     * Grants a service account access to a namespace.
     *
     * <p>Fetches the service account first to obtain the current {@code resourceVersion}.
     *
     * @param namespace the full namespace ID (e.g. "my-ns.acctid")
     * @param serviceAccountId the service account ID (e.g. "sa-xxxx")
     * @param permission one of "PERMISSION_ADMIN", "PERMISSION_WRITE", "PERMISSION_READ"
     */
    public JsonObject setServiceAccountNamespaceAccess(String namespace, String serviceAccountId,
            String permission) throws IOException, InterruptedException {
        // Fetch current resourceVersion — required by the API
        JsonObject sa = getServiceAccount(serviceAccountId);
        String resourceVersion = sa.getAsJsonObject("serviceAccount")
                .get("resourceVersion").getAsString();

        String requestBody = buildSetNamespaceAccessRequestBody(permission, resourceVersion);

        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH + "/" + namespace
                        + "/service-accounts/" + serviceAccountId + "/access")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Set namespace access");
    }

    /**
     * Adds custom search attributes to a namespace on Temporal Cloud. Fetches the current
     * namespace spec, merges in the new search attributes, and updates the namespace.
     *
     * <p>On Temporal Cloud, search attributes are managed via the Cloud Operations API's
     * {@code UpdateNamespace} endpoint — the Java SDK's {@code OperatorService} is not supported.
     *
     * @param namespaceId the full namespace ID (e.g. "my-ns.acctid")
     * @param searchAttributes map of attribute name to user-friendly type string (e.g. "Keyword",
     *     "Text", "Int", "Double", "Bool", "Datetime", "KeywordList")
     */
    public JsonObject addSearchAttributes(String namespaceId, Map<String, String> searchAttributes)
            throws IOException, InterruptedException {
        JsonObject ns = getNamespace(namespaceId);
        JsonObject nsObj = ns.getAsJsonObject("namespace");
        String resourceVersion = nsObj.get("resourceVersion").getAsString();

        JsonObject currentSpec =
                nsObj.has("spec") ? nsObj.getAsJsonObject("spec").deepCopy() : new JsonObject();

        // Get existing search attributes — flat map directly on the spec
        // v0.3.0+ uses "searchAttributes"; older uses "customSearchAttributes"
        JsonObject existingAttrs = new JsonObject();
        if (currentSpec.has("searchAttributes")
                && currentSpec.get("searchAttributes").isJsonObject()) {
            existingAttrs = currentSpec.getAsJsonObject("searchAttributes").deepCopy();
        } else if (currentSpec.has("customSearchAttributes")
                && currentSpec.get("customSearchAttributes").isJsonObject()) {
            existingAttrs = currentSpec.getAsJsonObject("customSearchAttributes").deepCopy();
        }

        // Merge new attributes
        for (Map.Entry<String, String> entry : searchAttributes.entrySet()) {
            existingAttrs.addProperty(entry.getKey(), toSearchAttributeType(entry.getValue()));
        }

        // Use the v0.3.0+ field name
        currentSpec.add("searchAttributes", existingAttrs);
        // Remove legacy field to avoid conflict
        currentSpec.remove("customSearchAttributes");

        String requestBody = buildUpdateRequestBody(currentSpec, resourceVersion);

        HttpRequest request =
                newRequestBuilder(NAMESPACES_PATH + "/" + namespaceId)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response, "Add search attributes");
    }

    /**
     * Gets the custom search attributes configured on a namespace.
     *
     * @param namespaceId the full namespace ID (e.g. "my-ns.acctid")
     * @return a JsonObject mapping attribute names to their type enum values
     */
    public JsonObject getSearchAttributes(String namespaceId)
            throws IOException, InterruptedException {
        JsonObject ns = getNamespace(namespaceId);
        return extractSearchAttributes(ns);
    }

    /**
     * Extracts custom search attributes from a namespace API response. Exposed for testing.
     *
     * @param namespaceResponse the full namespace response from the Cloud Operations API
     * @return a JsonObject mapping attribute names to their type enum values
     */
    public JsonObject extractSearchAttributes(JsonObject namespaceResponse) {
        JsonObject nsObj = namespaceResponse.getAsJsonObject("namespace");
        if (nsObj == null || !nsObj.has("spec")) {
            return new JsonObject();
        }
        JsonObject spec = nsObj.getAsJsonObject("spec");

        // v0.3.0+ uses "searchAttributes" as a flat map; older uses "customSearchAttributes"
        if (spec.has("searchAttributes")
                && spec.get("searchAttributes").isJsonObject()) {
            return spec.getAsJsonObject("searchAttributes");
        }
        if (spec.has("customSearchAttributes")
                && spec.get("customSearchAttributes").isJsonObject()) {
            return spec.getAsJsonObject("customSearchAttributes");
        }
        return new JsonObject();
    }

    /**
     * Maps a user-friendly search attribute type string to the Cloud Operations API integer enum.
     * Exposed for testing.
     *
     * <p>Supported types: Text (1), Keyword (2), Int (3), Double (4), Bool (5), Datetime (6),
     * KeywordList (7).
     *
     * @param userType the type string (case-insensitive)
     * @return the integer enum value
     * @throws IllegalArgumentException if the type is not recognized
     */
    public int toSearchAttributeType(String userType) {
        return switch (userType.toLowerCase()) {
            case "text" -> 1;
            case "keyword" -> 2;
            case "int" -> 3;
            case "double" -> 4;
            case "bool" -> 5;
            case "datetime" -> 6;
            case "keywordlist" -> 7;
            default -> throw new IllegalArgumentException(
                    "Unknown search attribute type: " + userType
                            + ". Supported: Text, Keyword, Int, Double, Bool, Datetime, KeywordList");
        };
    }

    /**
     * Builds the JSON request body for namespace creation. Exposed for testing.
     *
     * @param name the namespace name
     * @param region the Cloud region ID (e.g. "aws-us-east-1")
     */
    public String buildCreateRequestBody(String name, String region) {
        return buildCreateRequestBody(name, region, false);
    }

    /**
     * Builds the JSON request body for namespace creation, optionally enabling API key auth.
     * Exposed for testing.
     *
     * @param name the namespace name
     * @param region the Cloud region ID (e.g. "aws-us-east-1")
     * @param enableApiKeyAuth whether to include apiKeyAuth in the spec
     */
    public String buildCreateRequestBody(String name, String region, boolean enableApiKeyAuth) {
        JsonArray regions = new JsonArray();
        regions.add(region);

        JsonObject spec = new JsonObject();
        spec.addProperty("name", name);
        spec.add("regions", regions);
        spec.addProperty("retention_days", 30);

        if (enableApiKeyAuth) {
            JsonObject apiKeyAuth = new JsonObject();
            apiKeyAuth.addProperty("enabled", true);
            spec.add("apiKeyAuth", apiKeyAuth);
        }

        JsonObject body = new JsonObject();
        body.add("spec", spec);

        return gson.toJson(body);
    }

    /**
     * Builds the JSON request body for namespace update. Exposed for testing.
     *
     * @param spec the full namespace spec to send
     * @param resourceVersion the current resource version for optimistic concurrency
     */
    public String buildUpdateRequestBody(JsonObject spec, String resourceVersion) {
        JsonObject body = new JsonObject();
        body.add("spec", spec);
        body.addProperty("resourceVersion", resourceVersion);
        return gson.toJson(body);
    }

    /**
     * Builds the JSON request body for API key creation. Exposed for testing.
     *
     * @param displayName the display name for the API key
     * @param description optional description (may be null)
     * @param ownerType the owner type (e.g. "OWNER_TYPE_SERVICE_ACCOUNT")
     * @param ownerId the service account ID
     * @param expiryTime optional RFC 3339 expiry time (may be null)
     */
    public String buildCreateApiKeyRequestBody(String displayName, String description,
            String ownerType, String ownerId, String expiryTime) {
        JsonObject spec = new JsonObject();
        spec.addProperty("displayName", displayName);
        if (description != null) {
            spec.addProperty("description", description);
        }
        spec.addProperty("ownerType", ownerType);
        spec.addProperty("ownerId", ownerId);
        if (expiryTime != null) {
            spec.addProperty("expiryTime", expiryTime);
        }

        JsonObject body = new JsonObject();
        body.add("spec", spec);
        return gson.toJson(body);
    }

    /**
     * Builds the JSON request body for service account creation. Exposed for testing.
     *
     * @param name display name for the service account
     * @param description optional description (may be null)
     * @param accountRole the account-level role (e.g. "ROLE_READ")
     */
    public String buildCreateServiceAccountRequestBody(String name, String description,
            String accountRole) {
        JsonObject role = new JsonObject();
        role.addProperty("role", accountRole);

        JsonObject access = new JsonObject();
        access.add("accountAccess", role);

        JsonObject spec = new JsonObject();
        spec.addProperty("name", name);
        if (description != null) {
            spec.addProperty("description", description);
        }
        spec.add("access", access);

        JsonObject body = new JsonObject();
        body.add("spec", spec);
        return gson.toJson(body);
    }

    /**
     * Builds the JSON request body for setting namespace access on a service account.
     * Exposed for testing.
     *
     * @param permission the namespace permission enum (e.g. "PERMISSION_WRITE")
     * @param resourceVersion the current resource version of the service account
     */
    public String buildSetNamespaceAccessRequestBody(String permission, String resourceVersion) {
        JsonObject access = new JsonObject();
        access.addProperty("permission", permission);
        JsonObject body = new JsonObject();
        body.add("access", access);
        body.addProperty("resourceVersion", resourceVersion);
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
