package io.temporal.spring.boot.autoconfigure.byworkernameresolver;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.NexusOperationOptions;
import io.temporal.workflow.NexusServiceOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import org.springframework.context.annotation.Profile;

@WorkflowImpl(workers = "${resolver-test.worker-name}")
@Profile("auto-discovery-by-worker-name-resolver")
public class TestWorkflowImpl implements TestWorkflow {

  @Override
  public String execute(String input) {
    String nexusResult =
        Workflow.newNexusServiceStub(
                TestNexusService.class,
                NexusServiceOptions.newBuilder()
                    .setEndpoint("AutoDiscoveryByWorkerNameResolverTestEndpoint")
                    .setOperationOptions(
                        NexusOperationOptions.newBuilder()
                            .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                            .build())
                    .build())
            .operation(input);
    String activityResult =
        Workflow.newActivityStub(
                TestActivity.class,
                ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(1))
                    .validateAndBuildWithDefaults())
            .execute(input);
    return nexusResult + "|" + activityResult;
  }
}
