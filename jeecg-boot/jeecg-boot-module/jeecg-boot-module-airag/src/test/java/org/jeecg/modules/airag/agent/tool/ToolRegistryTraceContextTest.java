package org.jeecg.modules.airag.agent.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.trace.AgentToolTraceContextBridge;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

class ToolRegistryTraceContextTest {

    @Test
    void shouldOpenAndCloseTraceScopeAroundToolExecution() {
        ToolRegistry registry = new ToolRegistry();
        AtomicBoolean traceActive = new AtomicBoolean(false);
        AgentToolTraceContextBridge bridge = context -> {
            traceActive.set(true);
            return () -> traceActive.set(false);
        };
        ReflectionTestUtils.setField(registry, "traceContextBridge", bridge);

        ToolDefinition definition = new ToolDefinition();
        definition.setName("test_tool");
        definition.setExecutor((context, request) -> {
            Assertions.assertTrue(traceActive.get());
            return ToolCallResult.success("执行完成", null);
        });
        registry.register(definition);

        ToolCallRequest request = new ToolCallRequest();
        request.setToolName("test_tool");
        ToolCallResult result = registry.execute(new AgentContext(), request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(traceActive.get());
    }

    @Test
    void shouldCloseTraceScopeWhenToolExecutionFails() {
        ToolRegistry registry = new ToolRegistry();
        AtomicBoolean traceActive = new AtomicBoolean(false);
        AgentToolTraceContextBridge bridge = context -> {
            traceActive.set(true);
            return () -> traceActive.set(false);
        };
        ReflectionTestUtils.setField(registry, "traceContextBridge", bridge);

        ToolDefinition definition = new ToolDefinition();
        definition.setName("failed_tool");
        definition.setExecutor((context, request) -> {
            throw new IllegalStateException("执行失败");
        });
        registry.register(definition);

        ToolCallRequest request = new ToolCallRequest();
        request.setToolName("failed_tool");

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> registry.execute(new AgentContext(), request)
        );
        Assertions.assertFalse(traceActive.get());
    }
}
