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
}
