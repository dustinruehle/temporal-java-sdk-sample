package io.temporal.samples.shared;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TemporalCloudAdmin} that verify JSON request construction and response
 * parsing without making real HTTP calls.
 */
class TemporalCloudAdminTest {

    private TemporalCloudAdmin admin;

    @BeforeEach
    void setUp() {
        // Use a dummy API key and base URL — no real HTTP calls are made
        admin = new TemporalCloudAdmin("test-api-key", "https://localhost:0");
    }

    @Test
    void testBuildCreateRequestBody() {
        String json = admin.buildCreateRequestBody("my-namespace", "aws-us-east-1");

        JsonObject body = admin.parseResponse(json);
        assertTrue(body.has("spec"), "Body should contain 'spec'");

        JsonObject spec = body.getAsJsonObject("spec");
        assertEquals("my-namespace", spec.get("name").getAsString());

        assertTrue(spec.has("regions"), "Spec should contain 'regions'");
        assertEquals(1, spec.getAsJsonArray("regions").size());
        assertEquals("aws-us-east-1", spec.getAsJsonArray("regions").get(0).getAsString());

        assertEquals(30, spec.get("retention_days").getAsInt());
    }

    @Test
    void testBuildCreateRequestBodyDifferentRegion() {
        String json = admin.buildCreateRequestBody("prod-ns", "aws-eu-west-1");

        JsonObject body = admin.parseResponse(json);
        JsonObject spec = body.getAsJsonObject("spec");
        assertEquals("prod-ns", spec.get("name").getAsString());
        assertEquals("aws-eu-west-1", spec.getAsJsonArray("regions").get(0).getAsString());
    }

    @Test
    void testParseNamespaceResponse() {
        String responseJson =
                """
                {
                  "namespace": {
                    "namespace": "my-ns.acctid",
                    "state": "NAMESPACE_STATE_ACTIVE",
                    "spec": {
                      "name": "my-ns",
                      "region": "us-east-1",
                      "retention_period": { "days": 30 }
                    }
                  }
                }
                """;

        JsonObject result = admin.parseResponse(responseJson);
        assertTrue(result.has("namespace"));

        JsonObject ns = result.getAsJsonObject("namespace");
        assertEquals("my-ns.acctid", ns.get("namespace").getAsString());
        assertEquals("NAMESPACE_STATE_ACTIVE", ns.get("state").getAsString());
    }

    @Test
    void testParseListNamespacesResponse() {
        String responseJson =
                """
                {
                  "namespaces": [
                    { "namespace": "ns1.acctid", "state": "NAMESPACE_STATE_ACTIVE" },
                    { "namespace": "ns2.acctid", "state": "NAMESPACE_STATE_ACTIVE" }
                  ]
                }
                """;

        JsonObject result = admin.parseResponse(responseJson);
        assertTrue(result.has("namespaces"));
        assertEquals(2, result.getAsJsonArray("namespaces").size());
    }

    @Test
    void testParseEmptyListResponse() {
        String responseJson = "{ \"namespaces\": [] }";

        JsonObject result = admin.parseResponse(responseJson);
        assertTrue(result.has("namespaces"));
        assertEquals(0, result.getAsJsonArray("namespaces").size());
    }

    @Test
    void testParseDeleteResponse() {
        String responseJson =
                """
                {
                  "async_operation": {
                    "id": "op-67890",
                    "state": "pending"
                  }
                }
                """;

        JsonObject result = admin.parseResponse(responseJson);
        assertTrue(result.has("async_operation"));
        assertEquals("op-67890", result.getAsJsonObject("async_operation").get("id").getAsString());
        assertEquals("pending", result.getAsJsonObject("async_operation").get("state").getAsString());
    }

    @Test
    void testParseCreateResponse() {
        String responseJson =
                """
                {
                  "async_operation": {
                    "id": "op-12345",
                    "state": "pending"
                  },
                  "namespace": "new-ns.acctid"
                }
                """;

        JsonObject result = admin.parseResponse(responseJson);
        assertEquals("new-ns.acctid", result.get("namespace").getAsString());
        assertTrue(result.has("async_operation"));
        assertEquals("op-12345", result.getAsJsonObject("async_operation").get("id").getAsString());
    }

    @Test
    void testBuildCreateRequestBodyWithApiKeyAuth() {
        String json = admin.buildCreateRequestBody("my-namespace", "aws-us-east-1", true);

        JsonObject body = admin.parseResponse(json);
        JsonObject spec = body.getAsJsonObject("spec");
        assertEquals("my-namespace", spec.get("name").getAsString());

        assertTrue(spec.has("apiKeyAuth"), "Spec should contain 'apiKeyAuth'");
        JsonObject apiKeyAuth = spec.getAsJsonObject("apiKeyAuth");
        assertTrue(apiKeyAuth.get("enabled").getAsBoolean(), "apiKeyAuth should be enabled");
    }

    @Test
    void testBuildCreateRequestBodyWithoutApiKeyAuth() {
        String json = admin.buildCreateRequestBody("my-namespace", "aws-us-east-1", false);

        JsonObject body = admin.parseResponse(json);
        JsonObject spec = body.getAsJsonObject("spec");
        assertFalse(spec.has("apiKeyAuth"), "Spec should NOT contain 'apiKeyAuth' when disabled");
    }

    @Test
    void testBuildUpdateRequestBody() {
        JsonObject spec = new JsonObject();
        spec.addProperty("name", "my-ns");
        JsonObject apiKeyAuth = new JsonObject();
        apiKeyAuth.addProperty("enabled", true);
        spec.add("apiKeyAuth", apiKeyAuth);

        String json = admin.buildUpdateRequestBody(spec, "rv-123");

        JsonObject body = admin.parseResponse(json);
        assertTrue(body.has("spec"), "Body should contain 'spec'");
        assertTrue(body.has("resourceVersion"), "Body should contain 'resourceVersion'");
        assertEquals("rv-123", body.get("resourceVersion").getAsString());
        assertEquals("my-ns", body.getAsJsonObject("spec").get("name").getAsString());
        assertTrue(body.getAsJsonObject("spec").getAsJsonObject("apiKeyAuth").get("enabled").getAsBoolean());
    }

    @Test
    void testBuildCreateApiKeyRequestBody() {
        String json = admin.buildCreateApiKeyRequestBody(
                "worker-key", "A key for workers", "OWNER_TYPE_SERVICE_ACCOUNT", "sa-abc123",
                "2026-12-31T00:00:00Z");

        JsonObject body = admin.parseResponse(json);
        assertTrue(body.has("spec"), "Body should contain 'spec'");

        JsonObject spec = body.getAsJsonObject("spec");
        assertEquals("worker-key", spec.get("displayName").getAsString());
        assertEquals("A key for workers", spec.get("description").getAsString());
        assertEquals("OWNER_TYPE_SERVICE_ACCOUNT", spec.get("ownerType").getAsString());
        assertEquals("sa-abc123", spec.get("ownerId").getAsString());
        assertEquals("2026-12-31T00:00:00Z", spec.get("expiryTime").getAsString());
    }

    @Test
    void testBuildCreateApiKeyRequestBodyMinimal() {
        String json = admin.buildCreateApiKeyRequestBody(
                "worker-key", null, "OWNER_TYPE_SERVICE_ACCOUNT", "sa-abc123", null);

        JsonObject body = admin.parseResponse(json);
        JsonObject spec = body.getAsJsonObject("spec");

        assertEquals("worker-key", spec.get("displayName").getAsString());
        assertFalse(spec.has("description"), "Description should be omitted when null");
        assertEquals("OWNER_TYPE_SERVICE_ACCOUNT", spec.get("ownerType").getAsString());
        assertEquals("sa-abc123", spec.get("ownerId").getAsString());
        assertFalse(spec.has("expiryTime"), "ExpiryTime should be omitted when null");
    }

    @Test
    void testBuildCreateServiceAccountRequestBody() {
        String json = admin.buildCreateServiceAccountRequestBody(
                "my-worker-sa", "A service account for workers", "ROLE_DEVELOPER");

        JsonObject body = admin.parseResponse(json);
        assertTrue(body.has("spec"), "Body should contain 'spec'");

        JsonObject spec = body.getAsJsonObject("spec");
        assertEquals("my-worker-sa", spec.get("name").getAsString());
        assertEquals("A service account for workers", spec.get("description").getAsString());

        assertTrue(spec.has("access"), "Spec should contain 'access'");
        JsonObject access = spec.getAsJsonObject("access");
        assertTrue(access.has("accountAccess"), "Access should contain 'accountAccess'");
        assertEquals("ROLE_DEVELOPER", access.getAsJsonObject("accountAccess").get("role").getAsString());
    }

    @Test
    void testBuildCreateServiceAccountRequestBodyMinimal() {
        String json = admin.buildCreateServiceAccountRequestBody(
                "minimal-sa", null, "ROLE_READ");

        JsonObject body = admin.parseResponse(json);
        JsonObject spec = body.getAsJsonObject("spec");

        assertEquals("minimal-sa", spec.get("name").getAsString());
        assertFalse(spec.has("description"), "Description should be omitted when null");
        assertEquals("ROLE_READ",
                spec.getAsJsonObject("access").getAsJsonObject("accountAccess").get("role").getAsString());
    }

    @Test
    void testBuildSetNamespaceAccessRequestBody() {
        String json = admin.buildSetNamespaceAccessRequestBody("PERMISSION_WRITE", "rv-456");

        JsonObject body = admin.parseResponse(json);
        assertEquals("PERMISSION_WRITE", body.getAsJsonObject("access").get("permission").getAsString());
        assertEquals("rv-456", body.get("resourceVersion").getAsString());
    }

    @Test
    void testParseCreateServiceAccountResponse() {
        String responseJson =
                """
                {
                  "serviceAccountId": "sa-abc123",
                  "asyncOperation": {
                    "id": "op-55555",
                    "state": "fulfilled"
                  }
                }
                """;

        JsonObject result = admin.parseResponse(responseJson);
        assertEquals("sa-abc123", result.get("serviceAccountId").getAsString());
        assertTrue(result.has("asyncOperation"));
        assertEquals("op-55555", result.getAsJsonObject("asyncOperation").get("id").getAsString());
    }

    @Test
    void testParseCreateApiKeyResponse() {
        String responseJson =
                """
                {
                  "keyId": "key-12345",
                  "token": "tcsk_XXXXXXXXXX",
                  "asyncOperation": {
                    "id": "op-99999",
                    "state": "fulfilled"
                  }
                }
                """;

        JsonObject result = admin.parseResponse(responseJson);
        assertEquals("key-12345", result.get("keyId").getAsString());
        assertEquals("tcsk_XXXXXXXXXX", result.get("token").getAsString());
        assertTrue(result.has("asyncOperation"));
        assertEquals("op-99999", result.getAsJsonObject("asyncOperation").get("id").getAsString());
    }
}
