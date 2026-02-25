# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Temporal Java SDK **showcase project** demonstrating a broad range of workflow and activity patterns, concepts, and best practices using the Temporal Java SDK.

## Build & Environment

- **Build tool**: Gradle (Kotlin DSL)
- **Java version**: 21
- **Temporal SDK**: `io.temporal:temporal-sdk` (always use latest stable — check docs)
- **Testing**: `io.temporal:temporal-testing` with JUnit 5
- **Local Temporal**: `temporal server start-dev`
- **Temporal Web UI**: http://localhost:8233
- **Namespace**: `default` (local) or via env `TEMPORAL_NAMESPACE`

## Project Structure

```
src/main/java/io/temporal/samples/
  workflows/        # Workflow interface + implementation pairs
  activities/       # Activity interface + implementation pairs
  workers/          # Worker startup classes (each has a main())
  starters/         # Workflow client/starter demos
  interceptors/     # Custom interceptor demos
  dataconverters/   # Custom data converter demos
```

## Conventions

- Each concept lives in its own sub-package
- Every Worker class is independently runnable with its own `main()` method
- Task queues named: `showcase-<concept>` (e.g., `showcase-signals`)
- Run the Worker main class before the Starter when testing a concept
- No hardcoded credentials — use environment variables

## Concepts to Showcase

- [ ] Basic workflow + activity (Hello World)
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

## References

Use these as **one input among several** — not as the only source of truth:

- **Temporal samples-java** (https://github.com/temporalio/samples-java): Good reference for patterns and code structure, but check the Temporal docs and SDK changelog to ensure any patterns used are current and idiomatic for the SDK version in this project.
- **Temporal Java SDK docs**: Always prefer official docs over sample code when there's a conflict.
- **Temporal SDK Javadoc**: Use for API accuracy.

When implementing a concept, consult all three and use your judgment — samples-java may lag behind the latest SDK features.

## Status

Project is in initial setup phase. Source code and build files are being established.
Update this file as the project evolves.