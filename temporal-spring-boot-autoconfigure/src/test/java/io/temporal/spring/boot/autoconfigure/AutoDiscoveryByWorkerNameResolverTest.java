package io.temporal.spring.boot.autoconfigure;

import io.temporal.api.nexus.v1.Endpoint;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.spring.boot.autoconfigure.byworkernameresolver.TestWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that Spring property placeholders in the {@code workers} attribute of {@code
 * WorkflowImpl}, {@code ActivityImpl} and {@code NexusServiceImpl} are resolved.
 */
@SpringBootTest(classes = AutoDiscoveryByWorkerNameResolverTest.Configuration.class)
@ActiveProfiles(profiles = "auto-discovery-by-worker-name-resolver")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AutoDiscoveryByWorkerNameResolverTest {
  @Autowired ConfigurableApplicationContext applicationContext;

  @Autowired TestWorkflowEnvironment testWorkflowEnvironment;

  @Autowired WorkflowClient workflowClient;

  Endpoint endpoint;

  @BeforeEach
  void setUp() {
    applicationContext.start();
    endpoint =
        testWorkflowEnvironment.createNexusEndpoint(
            "AutoDiscoveryByWorkerNameResolverTestEndpoint", "WorkerNameResolverTest");
  }

  @AfterEach
  void tearDown() {
    testWorkflowEnvironment.deleteNexusEndpoint(endpoint);
  }

  @Test
  @Timeout(value = 10)
  public void testWorkerNamePlaceholdersAreResolved() {
    TestWorkflow testWorkflow =
        workflowClient.newWorkflowStub(
            TestWorkflow.class,
            WorkflowOptions.newBuilder().setTaskQueue("WorkerNameResolverTest").build());
    Assertions.assertEquals("Hello, input!|input", testWorkflow.execute("input"));
  }

  @ComponentScan(
      excludeFilters =
          @ComponentScan.Filter(
              pattern =
                  "io\\.temporal\\.spring\\.boot\\.autoconfigure\\.(bytaskqueue|byworkername)\\..*",
              type = FilterType.REGEX))
  public static class Configuration {}
}
