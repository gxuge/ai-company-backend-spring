package org.jeecg.modules.system.monitor;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.trace.AgentToolTraceContextBridge;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 将 AgentContext 中的 ts_ai_log 标识恢复到 Tool 当前执行线程。
 */
@Component
public class TsAiLogToolTraceContextBridge implements AgentToolTraceContextBridge {

    private static final String ATTR_LOG_ID = "tsAiLogId";
    private static final String ATTR_TRACE_ID = "tsAiLogTraceId";
    private static final String ATTR_STEP_COUNTER = "tsAiLogStepCounter";
    private static final int DEFAULT_ASYNC_STEP_START = 10;

    @Override
    public Scope open(AgentContext context) {
        if (TsAiLogTraceContext.isActive()) {
            return Scope.NOOP;
        }
        Long logId = resolveLogId(context);
        String traceId = resolveTraceId(context);
        if (logId == null || !StringUtils.hasText(traceId)) {
            return Scope.NOOP;
        }

        AtomicInteger stepCounter = resolveStepCounter(context);
        TsAiLogTraceContext.State previousState = TsAiLogTraceContext.get();
        TsAiLogTraceContext.set(new TsAiLogTraceContext.State(logId, traceId, stepCounter));
        return () -> restore(previousState);
    }

    /**
     * 读取异步 Agent Run 绑定的日志主键。
     */
    private Long resolveLogId(AgentContext context) {
        Object value = context == null ? null : context.getAttribute(ATTR_LOG_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 读取异步 Agent Run 绑定的 Trace ID。
     */
    private String resolveTraceId(AgentContext context) {
        Object value = context == null ? null : context.getAttribute(ATTR_TRACE_ID);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 复用 LlmNode 直写监控使用的步骤计数器。
     */
    private AtomicInteger resolveStepCounter(AgentContext context) {
        AtomicInteger counter = context == null
                ? null
                : context.getAttribute(ATTR_STEP_COUNTER, AtomicInteger.class);
        if (counter != null) {
            return counter;
        }
        AtomicInteger created = new AtomicInteger(DEFAULT_ASYNC_STEP_START);
        if (context != null) {
            context.putAttribute(ATTR_STEP_COUNTER, created);
        }
        return created;
    }

    /**
     * Tool 执行完成后恢复原线程状态，避免线程池复用造成串话。
     */
    private void restore(TsAiLogTraceContext.State previousState) {
        if (previousState == null) {
            TsAiLogTraceContext.clear();
            return;
        }
        TsAiLogTraceContext.set(previousState);
    }
}
