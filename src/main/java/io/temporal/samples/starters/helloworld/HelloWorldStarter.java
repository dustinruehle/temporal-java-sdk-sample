package io.temporal.samples.starters.helloworld;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.shared.TemporalCloudClient;
import io.temporal.samples.workflows.helloworld.HelloWorldWorkflow;

public class HelloWorldStarter {

    private static final String TASK_QUEUE = "showcase-helloworld";

    public static void main(String[] args) {
        TemporalCloudClient cloudClient = new TemporalCloudClient();
        WorkflowClient client = cloudClient.getClient();

        HelloWorldWorkflow workflow =
                client.newWorkflowStub(
                        HelloWorldWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("hello-world-" + System.currentTimeMillis())
                                .build());

        String result = workflow.sayHello("Temporal Cloud");
        System.out.println("Workflow result: " + result);

        System.exit(0);
    }
}
