package io.temporal.samples.workflows.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.activities.helloworld.HelloWorldActivities;
import io.temporal.samples.activities.helloworld.HelloWorldActivitiesImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelloWorldWorkflowTest {

    private static final String TASK_QUEUE = "test-helloworld";

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HelloWorldWorkflowImpl.class);
        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void testSayHelloWithRealActivity() {
        worker.registerActivitiesImplementations(new HelloWorldActivitiesImpl());
        testEnv.start();

        HelloWorldWorkflow workflow =
                client.newWorkflowStub(
                        HelloWorldWorkflow.class,
                        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        String result = workflow.sayHello("World");
        assertEquals("Hello, World!", result);
    }

    @Test
    void testSayHelloWithMockedActivity() {
        HelloWorldActivities mockedActivities = mock(HelloWorldActivities.class);
        when(mockedActivities.greet("World")).thenReturn("Mocked Hello, World!");

        worker.registerActivitiesImplementations(mockedActivities);
        testEnv.start();

        HelloWorldWorkflow workflow =
                client.newWorkflowStub(
                        HelloWorldWorkflow.class,
                        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        String result = workflow.sayHello("World");
        assertEquals("Mocked Hello, World!", result);
        verify(mockedActivities).greet("World");
    }
}
