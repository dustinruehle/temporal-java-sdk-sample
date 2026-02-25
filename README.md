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
│   └── helloworld/
├── shared/            # Shared utilities (e.g., TemporalCloudClient)
├── starters/          # Workflow starter classes (each has a main())
│   └── helloworld/
├── workers/           # Worker startup classes (each has a main())
│   └── helloworld/
└── workflows/         # Workflow interfaces + implementations
    └── helloworld/
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

## Concepts Roadmap

- [x] Basic workflow + activity (Hello World)
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
