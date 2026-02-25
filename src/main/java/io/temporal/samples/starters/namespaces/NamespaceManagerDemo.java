package io.temporal.samples.starters.namespaces;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.temporal.samples.shared.TemporalCloudAdmin;

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
                createNamespace(admin, name, region);
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

    private static void createNamespace(TemporalCloudAdmin admin, String name, String region)
            throws Exception {
        System.out.println("=== Creating namespace: " + name + " in " + region + " ===");

        JsonObject createResult = admin.createNamespace(name, region);
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

    private static void deleteNamespace(TemporalCloudAdmin admin, String namespaceId)
            throws Exception {
        System.out.println("=== Deleting namespace: " + namespaceId + " ===");
        System.out.println("Fetching current resource version...");

        JsonObject result = admin.deleteNamespace(namespaceId);
        System.out.println("Delete response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  --args=\"list\"                                    List all namespaces");
        System.err.println("  --args=\"create <name> [region]\"                  Create a namespace");
        System.err.println("  --args=\"delete <namespace-id>\"                   Delete a namespace");
        System.err.println();
        System.err.println("Region defaults to " + DEFAULT_REGION + " if not specified.");
        System.err.println("Namespace ID is the full name (e.g. my-ns.acctid).");
        System.err.println();
        System.err.println("Examples:");
        System.err.println(
                "  ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo"
                        + " --args=\"list\"");
        System.err.println(
                "  ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo"
                        + " --args=\"create my-namespace aws-us-east-1\"");
        System.err.println(
                "  ./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo"
                        + " --args=\"delete my-namespace.acctid\"");
    }
}
