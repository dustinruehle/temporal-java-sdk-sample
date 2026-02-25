package io.temporal.samples.starters.namespaces;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.temporal.samples.shared.TemporalCloudAdmin;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Demonstrates programmatic namespace management on Temporal Cloud using the Cloud Operations API.
 *
 * <p>Usage:
 *
 * <pre>
 * export TEMPORAL_CLOUD_API_KEY=&lt;admin-scoped-api-key&gt;
 *
 * # List namespaces:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="list"
 *
 * # Create a namespace:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="create my-new-namespace aws-us-east-1"
 *
 * # Create a namespace with API key auth enabled:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="create my-new-namespace aws-us-east-1 apikey"
 *
 * # Update a namespace to enable API key auth:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="update my-namespace.acctid enable-apikey-auth"
 *
 * # Create an API key for a service account:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="create-api-key worker-key sa-abc123"
 *
 * # Create a service account:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="create-service-account my-worker-sa"
 *
 * # Grant a service account access to a namespace:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="grant-ns-access my-namespace.acctid sa-xxxx NAMESPACE_WRITE"
 *
 * # One-shot setup (namespace + service account + access + API key):
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="setup my-namespace aws-us-east-1"
 *
 * # Delete a namespace:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
 *   --args="delete my-namespace.acctid"
 * </pre>
 */
public class NamespaceManagerDemo {

    private static final String DEFAULT_REGION = "aws-us-east-1";
    private static final int POLL_INTERVAL_SECONDS = 5;
    private static final int MAX_POLL_ATTEMPTS = 60;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        TemporalCloudAdmin admin = new TemporalCloudAdmin();

        switch (command) {
            case "list" -> listNamespaces(admin);
            case "create" -> {
                if (args.length < 2) {
                    System.err.println("Error: 'create' requires a namespace name.");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String name = args[1];
                String region = args.length >= 3 ? args[2] : DEFAULT_REGION;
                boolean enableApiKeyAuth = args.length >= 4 && "apikey".equalsIgnoreCase(args[3]);
                createNamespace(admin, name, region, enableApiKeyAuth);
            }
            case "update" -> {
                if (args.length < 3) {
                    System.err.println("Error: 'update' requires a namespace ID and an action.");
                    System.err.println("  Example: update my-ns.acctid enable-apikey-auth");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String namespaceId = args[1];
                String action = args[2];
                updateNamespace(admin, namespaceId, action);
            }
            case "create-api-key" -> {
                if (args.length < 3) {
                    System.err.println("Error: 'create-api-key' requires a display name and service account ID.");
                    System.err.println("  Example: create-api-key worker-key sa-abc123");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String displayName = args[1];
                String serviceAccountId = args[2];
                String expiryTime = args.length >= 4 ? args[3] : null;
                createApiKey(admin, displayName, serviceAccountId, expiryTime);
            }
            case "create-service-account" -> {
                if (args.length < 2) {
                    System.err.println("Error: 'create-service-account' requires a name.");
                    System.err.println("  Example: create-service-account my-worker-sa");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String saName = args[1];
                String accountRole = args.length >= 3 ? args[2] : "ROLE_READ";
                createServiceAccount(admin, saName, accountRole);
            }
            case "grant-ns-access" -> {
                if (args.length < 3) {
                    System.err.println("Error: 'grant-ns-access' requires a namespace ID and service account ID.");
                    System.err.println("  Example: grant-ns-access my-ns.acctid sa-xxxx NAMESPACE_WRITE");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String nsId = args[1];
                String saId = args[2];
                String permission = args.length >= 4 ? args[3] : "NAMESPACE_WRITE";
                grantNamespaceAccess(admin, nsId, saId, permission);
            }
            case "setup" -> {
                if (args.length < 2) {
                    System.err.println("Error: 'setup' requires a namespace name.");
                    System.err.println("  Example: setup my-namespace aws-us-east-1");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String nsName = args[1];
                String nsRegion = args.length >= 3 ? args[2] : DEFAULT_REGION;
                setupNamespace(admin, nsName, nsRegion);
            }
            case "delete" -> {
                if (args.length < 2) {
                    System.err.println("Error: 'delete' requires a namespace ID (e.g. my-ns.acctid).");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String namespaceId = args[1];
                deleteNamespace(admin, namespaceId);
            }
            default -> {
                System.err.println("Unknown command: " + command);
                System.err.println();
                printUsage();
                System.exit(1);
            }
        }

        System.exit(0);
    }

    private static void listNamespaces(TemporalCloudAdmin admin) throws Exception {
        System.out.println("=== Listing namespaces ===");
        JsonArray namespaces = admin.listNamespaces();
        if (namespaces.isEmpty()) {
            System.out.println("  (no namespaces found)");
        } else {
            for (int i = 0; i < namespaces.size(); i++) {
                JsonObject ns = namespaces.get(i).getAsJsonObject();
                String nsName = ns.has("namespace") ? ns.get("namespace").getAsString() : "unknown";
                System.out.println("  - " + nsName);
            }
            System.out.println();
            System.out.println("Total: " + namespaces.size() + " namespace(s)");
        }
    }

    private static void createNamespace(TemporalCloudAdmin admin, String name, String region,
            boolean enableApiKeyAuth) throws Exception {
        System.out.println("=== Creating namespace: " + name + " in " + region
                + (enableApiKeyAuth ? " (API key auth enabled)" : "") + " ===");

        JsonObject createResult = admin.createNamespace(name, region, enableApiKeyAuth);
        System.out.println("Create response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(createResult));

        // Use the full namespace ID from the response (e.g. "my-ns.acctid")
        String namespaceId = name;
        if (createResult.has("namespace")) {
            namespaceId = createResult.get("namespace").getAsString();
        }

        System.out.println();
        System.out.println("=== Polling for namespace readiness ===");
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            try {
                JsonObject ns = admin.getNamespace(namespaceId);
                String state = "";
                if (ns.has("namespace") && ns.getAsJsonObject("namespace").has("state")) {
                    state = ns.getAsJsonObject("namespace").get("state").getAsString();
                }

                System.out.println(
                        "  Attempt " + attempt + "/" + MAX_POLL_ATTEMPTS + " — state: " + state);

                if (state.contains("ACTIVE")) {
                    System.out.println();
                    System.out.println("Namespace is ACTIVE and ready to use!");
                    System.out.println(
                            new GsonBuilder().setPrettyPrinting().create().toJson(ns));
                    return;
                }
            } catch (Exception e) {
                System.out.println(
                        "  Attempt " + attempt + "/" + MAX_POLL_ATTEMPTS + " — " + e.getMessage());
            }

            if (attempt == MAX_POLL_ATTEMPTS) {
                System.out.println("Timed out waiting for namespace to become active.");
                return;
            }

            Thread.sleep(POLL_INTERVAL_SECONDS * 1000L);
        }
    }

    private static void updateNamespace(TemporalCloudAdmin admin, String namespaceId, String action)
            throws Exception {
        JsonObject specUpdates = new JsonObject();

        switch (action) {
            case "enable-apikey-auth" -> {
                JsonObject apiKeyAuth = new JsonObject();
                apiKeyAuth.addProperty("enabled", true);
                specUpdates.add("apiKeyAuth", apiKeyAuth);
            }
            case "disable-apikey-auth" -> {
                JsonObject apiKeyAuth = new JsonObject();
                apiKeyAuth.addProperty("enabled", false);
                specUpdates.add("apiKeyAuth", apiKeyAuth);
            }
            default -> {
                System.err.println("Unknown update action: " + action);
                System.err.println("Supported actions: enable-apikey-auth, disable-apikey-auth");
                System.exit(1);
            }
        }

        System.out.println("=== Updating namespace: " + namespaceId + " (" + action + ") ===");
        JsonObject result = admin.updateNamespace(namespaceId, specUpdates);
        System.out.println("Update response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    private static void createApiKey(TemporalCloudAdmin admin, String displayName,
            String serviceAccountId, String expiryTime) throws Exception {
        if (expiryTime == null) {
            expiryTime = Instant.now().plus(90, ChronoUnit.DAYS).toString();
            System.out.println("No expiry specified — defaulting to 90 days: " + expiryTime);
        }

        System.out.println("=== Creating API key: " + displayName + " ===");
        System.out.println("  Service account: " + serviceAccountId);
        System.out.println("  Expires: " + expiryTime);
        System.out.println();

        JsonObject result = admin.createApiKey(
                displayName, null, "OWNER_TYPE_SERVICE_ACCOUNT", serviceAccountId, expiryTime);

        System.out.println("Create API key response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));

        if (result.has("token")) {
            System.out.println();
            System.out.println("!!! IMPORTANT: Save the token below — it will NOT be shown again !!!");
            System.out.println("    Token: " + result.get("token").getAsString());
        }
    }

    private static void createServiceAccount(TemporalCloudAdmin admin, String name,
            String accountRole) throws Exception {
        System.out.println("=== Creating service account: " + name + " (role: " + accountRole + ") ===");

        JsonObject result = admin.createServiceAccount(name, null, accountRole);
        System.out.println("Create service account response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));

        if (result.has("serviceAccountId")) {
            System.out.println();
            System.out.println("Service account ID: " + result.get("serviceAccountId").getAsString());
        }
    }

    private static void grantNamespaceAccess(TemporalCloudAdmin admin, String namespaceId,
            String serviceAccountId, String permission) throws Exception {
        System.out.println("=== Granting namespace access ===");
        System.out.println("  Namespace:       " + namespaceId);
        System.out.println("  Service account: " + serviceAccountId);
        System.out.println("  Permission:      " + permission);
        System.out.println();

        JsonObject result = admin.setServiceAccountNamespaceAccess(
                namespaceId, serviceAccountId, permission);
        System.out.println("Grant access response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    private static void setupNamespace(TemporalCloudAdmin admin, String name, String region)
            throws Exception {
        System.out.println("=== Full namespace setup: " + name + " in " + region + " ===");
        System.out.println();

        // Step 1: Create namespace with API key auth
        System.out.println("[1/5] Creating namespace with API key auth...");
        JsonObject createResult = admin.createNamespace(name, region, true);
        String namespaceId = name;
        if (createResult.has("namespace")) {
            namespaceId = createResult.get("namespace").getAsString();
        }
        System.out.println("  Namespace ID: " + namespaceId);

        // Step 2: Poll until ACTIVE
        System.out.println();
        System.out.println("[2/5] Waiting for namespace to become ACTIVE...");
        boolean active = false;
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            try {
                JsonObject ns = admin.getNamespace(namespaceId);
                String state = "";
                if (ns.has("namespace") && ns.getAsJsonObject("namespace").has("state")) {
                    state = ns.getAsJsonObject("namespace").get("state").getAsString();
                }
                System.out.println(
                        "  Attempt " + attempt + "/" + MAX_POLL_ATTEMPTS + " — state: " + state);
                if (state.contains("ACTIVE")) {
                    active = true;
                    break;
                }
            } catch (Exception e) {
                System.out.println(
                        "  Attempt " + attempt + "/" + MAX_POLL_ATTEMPTS + " — " + e.getMessage());
            }
            if (attempt < MAX_POLL_ATTEMPTS) {
                Thread.sleep(POLL_INTERVAL_SECONDS * 1000L);
            }
        }
        if (!active) {
            System.err.println("Timed out waiting for namespace to become ACTIVE. Aborting setup.");
            return;
        }
        System.out.println("  Namespace is ACTIVE!");

        // Step 3: Create service account
        String saName = "sa-" + name;
        System.out.println();
        System.out.println("[3/5] Creating service account: " + saName + "...");
        JsonObject saResult = admin.createServiceAccount(saName, "Service account for " + name, "ROLE_READ");
        String serviceAccountId = saResult.has("serviceAccountId")
                ? saResult.get("serviceAccountId").getAsString()
                : null;
        if (serviceAccountId == null) {
            System.err.println("Failed to get service account ID from response:");
            System.err.println(new GsonBuilder().setPrettyPrinting().create().toJson(saResult));
            return;
        }
        System.out.println("  Service account ID: " + serviceAccountId);

        // Step 4: Wait for service account to be available, then grant namespace access
        System.out.println();
        System.out.println("[4/5] Granting NAMESPACE_WRITE access...");
        System.out.println("  Waiting for service account to be available...");
        boolean saReady = false;
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            try {
                admin.getServiceAccount(serviceAccountId);
                saReady = true;
                break;
            } catch (Exception e) {
                System.out.println(
                        "  Attempt " + attempt + "/" + MAX_POLL_ATTEMPTS + " — " + e.getMessage());
            }
            if (attempt < MAX_POLL_ATTEMPTS) {
                Thread.sleep(POLL_INTERVAL_SECONDS * 1000L);
            }
        }
        if (!saReady) {
            System.err.println("Timed out waiting for service account to become available. Aborting setup.");
            return;
        }
        admin.setServiceAccountNamespaceAccess(namespaceId, serviceAccountId, "NAMESPACE_WRITE");
        System.out.println("  Access granted.");

        // Step 5: Create API key
        String keyName = "key-" + name;
        String expiryTime = Instant.now().plus(90, ChronoUnit.DAYS).toString();
        System.out.println();
        System.out.println("[5/5] Creating API key: " + keyName + " (expires " + expiryTime + ")...");
        JsonObject keyResult = admin.createApiKey(
                keyName, "API key for " + name, "OWNER_TYPE_SERVICE_ACCOUNT",
                serviceAccountId, expiryTime);

        String token = keyResult.has("token") ? keyResult.get("token").getAsString() : null;
        String keyId = keyResult.has("keyId") ? keyResult.get("keyId").getAsString() : null;

        // Print summary
        System.out.println();
        System.out.println("=== Setup complete! ===");
        System.out.println();
        System.out.println("  Namespace:          " + namespaceId);
        System.out.println("  Service account:    " + serviceAccountId);
        System.out.println("  API key ID:         " + (keyId != null ? keyId : "(see response)"));
        if (token != null) {
            System.out.println();
            System.out.println("  !!! IMPORTANT: Save the token below — it will NOT be shown again !!!");
            System.out.println("  Token: " + token);
        }
    }

    private static void deleteNamespace(TemporalCloudAdmin admin, String namespaceId)
            throws Exception {
        System.out.println("=== Deleting namespace: " + namespaceId + " ===");
        System.out.println("Fetching current resource version...");

        JsonObject result = admin.deleteNamespace(namespaceId);
        System.out.println("Delete response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    private static void printUsage() {
        String mainClass = "io.temporal.samples.starters.namespaces.NamespaceManagerDemo";
        System.err.println("Usage:");
        System.err.println("  --args=\"list\"                                                          List all namespaces");
        System.err.println("  --args=\"create <name> [region] [apikey]\"                               Create a namespace");
        System.err.println("  --args=\"update <namespace-id> <action>\"                                Update a namespace");
        System.err.println("  --args=\"create-api-key <name> <sa-id> [expiry-RFC3339]\"                Create an API key");
        System.err.println("  --args=\"create-service-account <name> [account-role]\"                  Create a service account");
        System.err.println("  --args=\"grant-ns-access <namespace-id> <sa-id> [permission]\"           Grant SA namespace access");
        System.err.println("  --args=\"setup <name> [region]\"                                        Full setup (ns + SA + access + key)");
        System.err.println("  --args=\"delete <namespace-id>\"                                        Delete a namespace");
        System.err.println();
        System.err.println("Region defaults to " + DEFAULT_REGION + " if not specified.");
        System.err.println("Namespace ID is the full name (e.g. my-ns.acctid).");
        System.err.println("Update actions: enable-apikey-auth, disable-apikey-auth");
        System.err.println("API key expiry defaults to 90 days from now if not specified.");
        System.err.println("Account role defaults to ROLE_READ. Options: ROLE_OWNER, ROLE_ADMIN, ROLE_DEVELOPER, ROLE_READ, etc.");
        System.err.println("Namespace permission defaults to NAMESPACE_WRITE. Options: NAMESPACE_ADMIN, NAMESPACE_WRITE, NAMESPACE_READ");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"list\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"create my-namespace aws-us-east-1\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"create my-namespace aws-us-east-1 apikey\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"update my-namespace.acctid enable-apikey-auth\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"create-api-key worker-key sa-abc123\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"create-api-key worker-key sa-abc123 2026-12-31T00:00:00Z\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"create-service-account my-worker-sa\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"create-service-account my-worker-sa ROLE_DEVELOPER\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"grant-ns-access my-ns.acctid sa-xxxx NAMESPACE_WRITE\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"setup my-namespace aws-us-east-1\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"delete my-namespace.acctid\"");
    }
}
