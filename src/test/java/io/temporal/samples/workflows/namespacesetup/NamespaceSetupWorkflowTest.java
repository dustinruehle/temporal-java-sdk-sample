package io.temporal.samples.workflows.namespacesetup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.activities.namespacesetup.ApiKeyResult;
import io.temporal.samples.activities.namespacesetup.NamespaceSetupActivities;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamespaceSetupWorkflowTest {

    private static final String TASK_QUEUE = "test-namespace-setup";

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;
    private NamespaceSetupActivities mockedActivities;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(NamespaceSetupWorkflowImpl.class);
        client = testEnv.getWorkflowClient();
        mockedActivities = mock(NamespaceSetupActivities.class);
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void testHappyPath() {
        when(mockedActivities.createNamespace("test-ns", "aws-us-east-1"))
                .thenReturn("test-ns.acct123");

        // Polling activities are now blocking — just doNothing (success)
        doNothing().when(mockedActivities).waitForNamespaceActive("test-ns.acct123");

        when(mockedActivities.createServiceAccount("sa-test-ns"))
                .thenReturn("sa-abc123");

        doNothing().when(mockedActivities).waitForServiceAccountReady("sa-abc123");

        doNothing().when(mockedActivities).grantNamespaceAccess("test-ns.acct123", "sa-abc123");

        when(mockedActivities.createApiKey("key-test-ns", "sa-abc123"))
                .thenReturn(new ApiKeyResult("key-xyz789", "secret-token-value"));

        doNothing().when(mockedActivities).waitForNamespaceConnectivity("test-ns.acct123", "aws-us-east-1", "secret-token-value");

        when(mockedActivities.runHelloWorldWorkflow("test-ns.acct123", "aws-us-east-1", "secret-token-value"))
                .thenReturn("Hello, Namespace Setup!");

        worker.registerActivitiesImplementations(mockedActivities);
        testEnv.start();

        NamespaceSetupWorkflow workflow =
                client.newWorkflowStub(
                        NamespaceSetupWorkflow.class,
                        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        NamespaceSetupResult result = workflow.setup(new NamespaceSetupInput("test-ns", "aws-us-east-1"));

        // Verify result
        assertEquals("test-ns.acct123", result.namespaceId());
        assertEquals("sa-abc123", result.serviceAccountId());
        assertEquals("key-xyz789", result.apiKeyId());
        assertEquals("Hello, Namespace Setup!", result.helloWorldResult());

        // Verify activity call order
        var inOrder = inOrder(mockedActivities);
        inOrder.verify(mockedActivities).createNamespace("test-ns", "aws-us-east-1");
        inOrder.verify(mockedActivities).waitForNamespaceActive("test-ns.acct123");
        inOrder.verify(mockedActivities).createServiceAccount("sa-test-ns");
        inOrder.verify(mockedActivities).waitForServiceAccountReady("sa-abc123");
        inOrder.verify(mockedActivities).grantNamespaceAccess("test-ns.acct123", "sa-abc123");
        inOrder.verify(mockedActivities).createApiKey("key-test-ns", "sa-abc123");
        inOrder.verify(mockedActivities).waitForNamespaceConnectivity("test-ns.acct123", "aws-us-east-1", "secret-token-value");
        inOrder.verify(mockedActivities).runHelloWorldWorkflow("test-ns.acct123", "aws-us-east-1", "secret-token-value");
    }

    @Test
    void testResultContainsAllFields() {
        when(mockedActivities.createNamespace("fields-ns", "aws-eu-west-1"))
                .thenReturn("fields-ns.acct789");
        doNothing().when(mockedActivities).waitForNamespaceActive("fields-ns.acct789");
        when(mockedActivities.createServiceAccount("sa-fields-ns"))
                .thenReturn("sa-ghi789");
        doNothing().when(mockedActivities).waitForServiceAccountReady("sa-ghi789");
        doNothing().when(mockedActivities).grantNamespaceAccess("fields-ns.acct789", "sa-ghi789");
        when(mockedActivities.createApiKey("key-fields-ns", "sa-ghi789"))
                .thenReturn(new ApiKeyResult("key-fields123", "fields-token"));
        doNothing().when(mockedActivities).waitForNamespaceConnectivity("fields-ns.acct789", "aws-eu-west-1", "fields-token");
        when(mockedActivities.runHelloWorldWorkflow("fields-ns.acct789", "aws-eu-west-1", "fields-token"))
                .thenReturn("Hello, Namespace Setup!");

        worker.registerActivitiesImplementations(mockedActivities);
        testEnv.start();

        NamespaceSetupWorkflow workflow =
                client.newWorkflowStub(
                        NamespaceSetupWorkflow.class,
                        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

        NamespaceSetupResult result = workflow.setup(new NamespaceSetupInput("fields-ns", "aws-eu-west-1"));

        assertNotNull(result.namespaceId());
        assertNotNull(result.serviceAccountId());
        assertNotNull(result.apiKeyId());
        assertNotNull(result.helloWorldResult());
        assertFalse(result.namespaceId().isEmpty());
        assertFalse(result.serviceAccountId().isEmpty());
        assertFalse(result.apiKeyId().isEmpty());
        assertTrue(result.helloWorldResult().contains("Hello"));
    }
}
