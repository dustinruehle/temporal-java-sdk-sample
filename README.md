# Temporal Java SDK Showcase

A showcase project demonstrating workflow and activity patterns using the Temporal Java SDK, connected to Temporal Cloud.

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Gradle | Included via wrapper (`./gradlew`) |
| Temporal Cloud | Account with an API key |

## Project Structure

```
src/main/java/io/temporal/samples/
├── activities/        # Activity interfaces + implementations
│   ├── helloworld/
│   └── namespacesetup/
├── shared/            # Shared utilities (TemporalCloudClient, TemporalCloudAdmin)
├── starters/          # Workflow starter classes (each has a main())
│   ├── helloworld/
│   ├── namespaces/
│   └── namespacesetup/
├── workers/           # Worker startup classes (each has a main())
│   ├── helloworld/
│   └── namespacesetup/
└── workflows/         # Workflow interfaces + implementations
    ├── helloworld/
    └── namespacesetup/
```

## Setup

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd temporal-java-sdk-sample
   ```

2. Create a `.env` file in the project root with your Temporal Cloud credentials:
   ```
   TEMPORAL_ADDRESS=<region>.aws.api.temporal.io:7233
   TEMPORAL_NAMESPACE=<your-namespace>.<account-id>
   TEMPORAL_API_KEY=<your-api-key>
   ```

   | Variable | Description |
   |---|---|
   | `TEMPORAL_ADDRESS` | Temporal Cloud gRPC endpoint (e.g., `us-east-1.aws.api.temporal.io:7233`) |
   | `TEMPORAL_NAMESPACE` | Your Cloud namespace in `name.accountId` format |
   | `TEMPORAL_API_KEY` | API key from Temporal Cloud (raw key, no `Bearer` prefix) |

3. Load the environment variables:
   ```bash
   export $(cat .env | xargs)
   ```

## Running the Hello World

You need **two terminals** — one for the worker, one for the starter. Load env vars in both.

**Terminal 1 — Start the worker:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.workers.helloworld.HelloWorldWorker
```
You should see: `HelloWorldWorker started, polling task queue: showcase-helloworld`

**Terminal 2 — Run the starter:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.starters.helloworld.HelloWorldStarter
```
Expected output:
```
Workflow result: Hello, Temporal Cloud!
```

## Running Tests

Tests use the Temporal test framework with an in-memory server — no Cloud connection needed.

```bash
./gradlew test
```

## How It Works

The Hello World example follows the standard Temporal workflow → activity pattern:

1. **`HelloWorldStarter`** creates a `WorkflowClient` connected to Temporal Cloud and starts the `sayHello` workflow
2. **`HelloWorldWorkflow`** receives the call and delegates to the `greet` activity
3. **`HelloWorldActivities.greet()`** returns `"Hello, <name>!"` back through the workflow
4. The starter receives the result and prints it

Key classes:

| Class | Role |
|---|---|
| `HelloWorldWorkflow` / `Impl` | Workflow interface and implementation |
| `HelloWorldActivities` / `Impl` | Activity interface and implementation |
| `HelloWorldWorker` | Registers workflow + activity, polls task queue |
| `HelloWorldStarter` | Creates and executes the workflow |
| `TemporalCloudClient` | Shared utility for Temporal Cloud API key auth |

## Namespace Management (Cloud Operations API)

This example demonstrates programmatic namespace creation on Temporal Cloud using the [Cloud Operations API](https://docs.temporal.io/ops). The Java SDK's `RegisterNamespace` gRPC call only works with self-hosted Temporal — for Cloud, you must use the Cloud Operations REST API.

**Setup:**

Add `TEMPORAL_CLOUD_API_KEY` to your `.env` file (this needs an admin-scoped API key with namespace management permissions — it may differ from your namespace-scoped `TEMPORAL_API_KEY`):
```
TEMPORAL_CLOUD_API_KEY=<admin-scoped-api-key>
```

**List namespaces:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="list"
```

**Create a namespace:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="create my-new-namespace aws-us-east-1"
```

The demo will create the namespace, then poll until it reaches `ACTIVE` state.

**Create a namespace with API key auth enabled:**
```bash
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="create my-new-namespace aws-us-east-1 apikey"
```

**Update a namespace (enable API key auth):**
```bash
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="update my-namespace.acctid enable-apikey-auth"
```

**Create a service account:**
```bash
# Default role (ROLE_READ):
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="create-service-account my-worker-sa"

# Explicit role:
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="create-service-account my-worker-sa ROLE_DEVELOPER"
```

The response will print a **service account ID** (e.g. `sa-abc123`). This is different from the display name you provided — subsequent commands (`grant-ns-access`, `create-api-key`) require this `sa-xxxxx` ID, not the display name. You can also find it in the Temporal Cloud UI under **Settings → Service Accounts**.

**Grant a service account access to a namespace:**
```bash
# Uses the sa-xxxxx ID, not the display name
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="grant-ns-access my-namespace.acctid sa-abc123 NAMESPACE_WRITE"
```

**Create an API key for a service account:**
```bash
# Uses the sa-xxxxx ID, not the display name. Default expiry (90 days):
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="create-api-key worker-key sa-abc123"

# Explicit expiry:
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="create-api-key worker-key sa-abc123 2026-12-31T00:00:00Z"
```

**One-shot setup (namespace + service account + access + API key):**
```bash
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="setup my-namespace aws-us-east-1"
```

This creates a namespace with API key auth, waits for it to become ACTIVE, creates a service account, grants it `NAMESPACE_WRITE` access, and creates an API key — all in one command.

**Delete a namespace:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.starters.namespaces.NamespaceManagerDemo \
  --args="delete my-namespace.acctid"
```

| Class | Role |
|---|---|
| `TemporalCloudAdmin` | HTTP client wrapper for the Cloud Operations API |
| `NamespaceManagerDemo` | Runnable demo that lists/creates/updates/deletes namespaces and manages API keys |

## Namespace Setup Workflow (Orchestration Demo)

This example reimplements the imperative `NamespaceManagerDemo.setup()` flow as a **Temporal workflow**. Each Cloud Operations API call becomes a granular activity, polling uses durable `Workflow.sleep`, and the final step connects to the newly created namespace to run a HelloWorld workflow on it.

**What it demonstrates:**
- Workflow orchestration of multi-step provisioning
- Granular activities for visibility and retryability
- Durable timers with `Workflow.sleep()` for polling loops
- Dynamic Temporal client creation (connecting to a freshly provisioned namespace at runtime)

**Architecture:**
```
[Existing Temporal instance]                    [Temporal Cloud - new namespace]
        │                                                  │
  NamespaceSetupWorker                              HelloWorldWorker
        │                                           (started in-process
  NamespaceSetupWorkflow                             by final activity)
   ├─ createNamespace()         ── REST ──►  Cloud Ops API
   ├─ Workflow.sleep + getNamespaceState() loop
   ├─ createServiceAccount()    ── REST ──►  Cloud Ops API
   ├─ Workflow.sleep + isServiceAccountReady() loop
   ├─ grantNamespaceAccess()    ── REST ──►  Cloud Ops API
   ├─ createApiKey()            ── REST ──►  Cloud Ops API
   └─ runHelloWorldWorkflow()   ── gRPC ──►  New namespace
```

**Setup:**

Ensure both `TEMPORAL_API_KEY` (for the existing Temporal instance) and `TEMPORAL_CLOUD_API_KEY` (admin-scoped, for Cloud Operations API) are in your `.env` file.

**Terminal 1 — Start the worker:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.workers.namespacesetup.NamespaceSetupWorker
```

**Terminal 2 — Run the starter:**
```bash
export $(cat .env | xargs)
./gradlew execute -PmainClass=io.temporal.samples.starters.namespacesetup.NamespaceSetupStarter \
  --args="my-test-ns aws-us-east-1"
```

Expected output:
```
=== Namespace Setup Complete ===
  Namespace:       my-test-ns.acctid
  Service Account: sa-abc123
  API Key ID:      key-xyz789
  HelloWorld:      Hello, Namespace Setup!
```

You can watch the workflow in the Temporal Web UI — each activity is visible as a separate event in the workflow history.

| Class | Role |
|---|---|
| `NamespaceSetupWorkflow` / `Impl` | Orchestration workflow — sequences all steps with durable sleep |
| `NamespaceSetupActivities` / `Impl` | Granular activities — one per Cloud API call + HelloWorld runner |
| `NamespaceSetupWorker` | Registers workflow + activities, polls task queue |
| `NamespaceSetupStarter` | Creates and executes the setup workflow |

## Concepts Roadmap

- [x] Basic workflow + activity (Hello World)
- [x] Programmatic namespace management (Cloud Operations API)
- [ ] Long-running workflows with heartbeating
- [ ] Child workflows
- [ ] Signals and Queries
- [ ] Updates (and UpdateWithStart)
- [ ] Schedules
- [ ] Worker Versioning
- [ ] Saga / compensation pattern
- [ ] Timers and `Workflow.sleep()`
- [ ] Side effects and `MutableSideEffect`
- [ ] Custom data converters
- [ ] OpenTelemetry interceptors
