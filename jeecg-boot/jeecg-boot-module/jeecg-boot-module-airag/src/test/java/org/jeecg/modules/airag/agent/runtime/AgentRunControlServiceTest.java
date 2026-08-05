package org.jeecg.modules.airag.agent.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentRunControlServiceTest {

    @Test
    void shouldValidateRunOwnershipAndMarkRunStopped() {
        AgentRunControlService service = new AgentRunControlService();
        AgentContext context = new AgentContext();
        context.setRunId("run-1");
        context.setSessionId(12L);
        context.setUserId("user-1");
        service.register(context);

        Assertions.assertEquals(
                AgentRunControlService.StopResult.NOT_FOUND,
                service.requestStop("run-1", 13L, "user-1")
        );
        Assertions.assertEquals(
                AgentRunControlService.StopResult.NOT_FOUND,
                service.requestStop("run-1", 12L, "user-2")
        );
        Assertions.assertEquals(
                AgentRunControlService.StopResult.STOP_REQUESTED,
                service.requestStop("run-1", 12L, "user-1")
        );
        Assertions.assertTrue(AgentRunControlService.isStopRequested(context));
        Assertions.assertEquals(
                AgentRunControlService.StopResult.ALREADY_STOPPED,
                service.requestStop("run-1", 12L, "user-1")
        );

        service.unregister(context);
        Assertions.assertEquals(
                AgentRunControlService.StopResult.NOT_FOUND,
                service.requestStop("run-1", 12L, "user-1")
        );
    }
}
