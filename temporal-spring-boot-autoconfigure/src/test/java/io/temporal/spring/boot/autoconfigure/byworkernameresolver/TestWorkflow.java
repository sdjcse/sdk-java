package io.temporal.spring.boot.autoconfigure.byworkernameresolver;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TestWorkflow {

  @WorkflowMethod(name = "resolverTestWorkflow")
  String execute(String input);
}
