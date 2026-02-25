package io.temporal.samples.workflows.helloworld;

import io.temporal.activity.ActivityOptions;
import io.temporal.samples.activities.helloworld.HelloWorldActivities;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class HelloWorldWorkflowImpl implements HelloWorldWorkflow {

    private final HelloWorldActivities activities =
            Workflow.newActivityStub(
                    HelloWorldActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(10))
                            .build());

    @Override
    public String sayHello(String name) {
        return activities.greet(name);
    }
}
