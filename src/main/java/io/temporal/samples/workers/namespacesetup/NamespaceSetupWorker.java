package io.temporal.samples.workers.namespacesetup;

import io.temporal.samples.activities.namespacesetup.NamespaceSetupActivitiesImpl;
import io.temporal.samples.shared.TemporalCloudAdmin;
import io.temporal.samples.shared.TemporalCloudClient;
import io.temporal.samples.workflows.namespacesetup.NamespaceSetupWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Worker for the namespace setup orchestration workflow.
 *
 * <p>Connects to the existing Temporal instance (local dev server or Cloud namespace) and polls
 * the {@code showcase-namespace-setup} task queue. Activities make REST calls to the Temporal Cloud
 * Operations API to provision a new namespace.
 *
 * <p>Requires environment variables:
 * <ul>
 *   <li>{@code TEMPORAL_ADDRESS}, {@code TEMPORAL_NAMESPACE}, {@code TEMPORAL_API_KEY} — for the
 *       existing Temporal instance</li>
 *   <li>{@code TEMPORAL_CLOUD_API_KEY} — admin-scoped API key for Cloud Operations API</li>
 * </ul>
 */
public class NamespaceSetupWorker {

    private static final String TASK_QUEUE = "showcase-namespace-setup";

    public static void main(String[] args) {
        TemporalCloudClient cloudClient = new TemporalCloudClient();
        TemporalCloudAdmin admin = new TemporalCloudAdmin();

        WorkerFactory factory = WorkerFactory.newInstance(cloudClient.getClient());
        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(NamespaceSetupWorkflowImpl.class);
        worker.registerActivitiesImplementations(new NamespaceSetupActivitiesImpl(admin));

        factory.start();
        System.out.println("NamespaceSetupWorker started, polling task queue: " + TASK_QUEUE);
    }
}
