package org.jeecg.modules.airag.llm.stream;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;

/**
 * 标记执行完成后应立即结束当前模型调用的 ToolExecutor。
 */
public final class ImmediateToolExecutor implements ToolExecutor {

    private final ToolExecutor delegate;

    private ImmediateToolExecutor(ToolExecutor delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        this.delegate = delegate;
    }

    /**
     * 包装一个需要立即返回的工具执行器。
     */
    public static ToolExecutor wrap(ToolExecutor delegate) {
        if (delegate instanceof ImmediateToolExecutor) {
            return delegate;
        }
        return new ImmediateToolExecutor(delegate);
    }

    /**
     * 判断执行器是否带有立即返回标记。
     */
    public static boolean isImmediate(ToolExecutor executor) {
        return executor instanceof ImmediateToolExecutor;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        return this.delegate.execute(request, memoryId);
    }
}
