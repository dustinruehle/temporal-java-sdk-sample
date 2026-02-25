package io.temporal.samples.workflows.namespacesetup;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Orchestrates the full setup of a Temporal Cloud namespace.
 *
 * <p>Each step (create namespace, poll for readiness, create service account, grant access, create
 * API key, run HelloWorld) is a separate activity — giving durable execution, visibility in the
 * Temporal Web UI, and automatic retries.
 */
@WorkflowInterface
public interface NamespaceSetupWorkflow {

    @WorkflowMethod
    NamespaceSetupResult setup(NamespaceSetupInput input);
}
