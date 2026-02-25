package io.temporal.samples.workers.helloworld;

import io.temporal.samples.activities.helloworld.HelloWorldActivitiesImpl;
import io.temporal.samples.shared.TemporalCloudClient;
import io.temporal.samples.workflows.helloworld.HelloWorldWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class HelloWorldWorker {

    private static final String TASK_QUEUE = "showcase-helloworld";

    public static void main(String[] args) {
        TemporalCloudClient cloudClient = new TemporalCloudClient();

        WorkerFactory factory = WorkerFactory.newInstance(cloudClient.getClient());
        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());

        factory.start();
        System.out.println("HelloWorldWorker started, polling task queue: " + TASK_QUEUE);
    }
}
