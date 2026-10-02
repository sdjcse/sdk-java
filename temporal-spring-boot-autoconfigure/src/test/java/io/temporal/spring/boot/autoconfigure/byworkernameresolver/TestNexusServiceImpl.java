package io.temporal.spring.boot.autoconfigure.byworkernameresolver;

import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.spring.boot.NexusServiceImpl;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("ResolverTestNexusServiceImpl")
@NexusServiceImpl(workers = "${resolver-test.worker-name}")
@ServiceImpl(service = TestNexusService.class)
@Profile("auto-discovery-by-worker-name-resolver")
public class TestNexusServiceImpl {
  @OperationImpl
  public OperationHandler<String, String> operation() {
    return OperationHandler.sync((ctx, details, name) -> "Hello, " + name + "!");
  }
}
