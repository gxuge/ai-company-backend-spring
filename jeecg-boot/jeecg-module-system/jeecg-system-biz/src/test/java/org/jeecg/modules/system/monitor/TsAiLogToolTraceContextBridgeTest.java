package org.jeecg.modules.system.monitor;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.trace.AgentToolTraceContextBridge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class TsAiLogToolTraceContextBridgeTest {

    private final TsAiLogToolTraceContextBridge bridge = new TsAiLogToolTraceContextBridge();

    @AfterEach
    void tearDown() {
        TsAiLogTraceContext.clear();
    }

    @Test
    void shouldRestoreAsyncTraceContextAndReuseStepCounter() {
        AgentContext context = new AgentContext();
        AtomicInteger stepCounter = new AtomicInteger(12);
        context.putAttribute("tsAiLogId", 1001L);
        context.putAttribute("tsAiLogTraceId", "trace-1001");
        context.putAttribute("tsAiLogStepCounter", stepCounter);

        AgentToolTraceContextBridge.Scope scope = this.bridge.open(context);

        Assertions.assertTrue(TsAiLogTraceContext.isActive());
        Assertions.assertEquals(1001L, TsAiLogTraceContext.get().getLogId());
        Assertions.assertEquals("trace-1001", TsAiLogTraceContext.get().getTraceId());
        Assertions.assertEquals(13, TsAiLogTraceContext.nextStepNo());
        Assertions.assertEquals(13, stepCounter.get());

        scope.close();
        Assertions.assertFalse(TsAiLogTraceContext.isActive());
    }

    @Test
    void shouldKeepExistingRequestThreadTraceContext() {
        TsAiLogTraceContext.State original = new TsAiLogTraceContext.State(2001L, "trace-2001");
        TsAiLogTraceContext.set(original);

        AgentContext context = new AgentContext();
        context.putAttribute("tsAiLogId", 3001L);
        context.putAttribute("tsAiLogTraceId", "trace-3001");

        AgentToolTraceContextBridge.Scope scope = this.bridge.open(context);

        Assertions.assertSame(original, TsAiLogTraceContext.get());
        scope.close();
        Assertions.assertSame(original, TsAiLogTraceContext.get());
    }
}
