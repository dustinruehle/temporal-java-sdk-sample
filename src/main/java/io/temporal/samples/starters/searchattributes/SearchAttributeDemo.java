package io.temporal.samples.starters.searchattributes;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.temporal.samples.shared.TemporalCloudAdmin;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demonstrates custom search attribute management on Temporal Cloud using the Cloud Operations API.
 *
 * <p>The Java SDK's {@code OperatorService.addSearchAttributes()} is <b>not supported on Temporal
 * Cloud</b> — it only works with self-hosted Temporal Server. Even if the {@code
 * NullPointerException} from standalone {@code OperatorServiceStubs} is fixed (by deriving it from
 * an existing {@code WorkflowServiceStubs} via {@code setChannel(service.getRawChannel())}), Cloud
 * returns {@code PERMISSION_DENIED}.
 *
 * <p>For Temporal Cloud, search attributes must be managed via the Cloud Operations API's {@code
 * UpdateNamespace} endpoint, which is what this demo implements.
 *
 * <p>Usage:
 *
 * <pre>
 * export TEMPORAL_CLOUD_API_KEY=&lt;admin-scoped-api-key&gt;
 *
 * # List search attributes on a namespace:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.searchattributes.SearchAttributeDemo \
 *   --args="list my-ns.acctid"
 *
 * # Add search attributes:
 * ./gradlew execute -PmainClass=io.temporal.samples.starters.searchattributes.SearchAttributeDemo \
 *   --args="add my-ns.acctid app_id=Keyword customer_name=Text priority=Int"
 * </pre>
 */
public class SearchAttributeDemo {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        TemporalCloudAdmin admin = new TemporalCloudAdmin();

        switch (command) {
            case "list" -> {
                if (args.length < 2) {
                    System.err.println("Error: 'list' requires a namespace ID.");
                    System.err.println("  Example: list my-ns.acctid");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                listSearchAttributes(admin, args[1]);
            }
            case "add" -> {
                if (args.length < 3) {
                    System.err.println(
                            "Error: 'add' requires a namespace ID and at least one name=type pair.");
                    System.err.println("  Example: add my-ns.acctid app_id=Keyword");
                    System.err.println();
                    printUsage();
                    System.exit(1);
                }
                String namespaceId = args[1];
                Map<String, String> attrs = new LinkedHashMap<>();
                for (int i = 2; i < args.length; i++) {
                    String[] parts = args[i].split("=", 2);
                    if (parts.length != 2) {
                        System.err.println("Error: Invalid format '" + args[i]
                                + "'. Expected name=type (e.g. app_id=Keyword).");
                        System.exit(1);
                    }
                    attrs.put(parts[0], parts[1]);
                }
                addSearchAttributes(admin, namespaceId, attrs);
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

    private static void listSearchAttributes(TemporalCloudAdmin admin, String namespaceId)
            throws Exception {
        System.out.println("=== Search attributes on " + namespaceId + " ===");

        JsonObject attrs = admin.getSearchAttributes(namespaceId);
        if (attrs.size() == 0) {
            System.out.println("  (no custom search attributes)");
        } else {
            for (Map.Entry<String, JsonElement> entry : attrs.entrySet()) {
                System.out.println("  " + entry.getKey() + " = "
                        + formatType(entry.getValue()));
            }
            System.out.println();
            System.out.println("Total: " + attrs.size() + " custom search attribute(s)");
        }
    }

    private static void addSearchAttributes(
            TemporalCloudAdmin admin, String namespaceId, Map<String, String> attrs)
            throws Exception {
        System.out.println("=== Adding search attributes to " + namespaceId + " ===");
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println();

        JsonObject result = admin.addSearchAttributes(namespaceId, attrs);
        System.out.println("Update response:");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    /** Formats a search attribute type value — handles both integer enums and string enum names. */
    private static String formatType(JsonElement value) {
        // API may return integer (e.g. 2) or string (e.g. "SEARCH_ATTRIBUTE_TYPE_KEYWORD")
        try {
            return typeEnumToString(value.getAsInt());
        } catch (NumberFormatException e) {
            String s = value.getAsString();
            // Strip the "SEARCH_ATTRIBUTE_TYPE_" prefix for readability
            if (s.startsWith("SEARCH_ATTRIBUTE_TYPE_")) {
                s = s.substring("SEARCH_ATTRIBUTE_TYPE_".length());
            }
            // Title-case it: "KEYWORD" -> "Keyword", "KEYWORD_LIST" -> "KeywordList"
            return toTitleCase(s);
        }
    }

    private static String toTitleCase(String enumName) {
        StringBuilder sb = new StringBuilder();
        for (String part : enumName.split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private static String typeEnumToString(int typeEnum) {
        return switch (typeEnum) {
            case 1 -> "Text";
            case 2 -> "Keyword";
            case 3 -> "Int";
            case 4 -> "Double";
            case 5 -> "Bool";
            case 6 -> "Datetime";
            case 7 -> "KeywordList";
            default -> "Unknown(" + typeEnum + ")";
        };
    }

    private static void printUsage() {
        String mainClass =
                "io.temporal.samples.starters.searchattributes.SearchAttributeDemo";
        System.err.println("Search Attribute Management for Temporal Cloud");
        System.err.println();
        System.err.println("Usage:");
        System.err.println(
                "  --args=\"list <namespace-id>\"                                List custom search attributes");
        System.err.println(
                "  --args=\"add <namespace-id> <name>=<type> [...]\"             Add search attributes");
        System.err.println();
        System.err.println("Supported types: Bool, Datetime, Double, Int, Keyword, Text, KeywordList");
        System.err.println();
        System.err.println("Note: The Java SDK's OperatorService.addSearchAttributes() is NOT supported");
        System.err.println("on Temporal Cloud. This demo uses the Cloud Operations API instead.");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"list my-ns.acctid\"");
        System.err.println("  ./gradlew execute -PmainClass=" + mainClass
                + " --args=\"add my-ns.acctid app_id=Keyword customer_name=Text\"");
    }
}
