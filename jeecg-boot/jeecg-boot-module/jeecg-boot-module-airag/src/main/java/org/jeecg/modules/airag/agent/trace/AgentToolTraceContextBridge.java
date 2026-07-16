package org.jeecg.modules.airag.agent.trace;

import org.jeecg.modules.airag.agent.runtime.AgentContext;

/**
 * Agent Tool 执行线程的 Trace 上下文桥接器。
 *
 * <p>airag 仅声明扩展点，具体监控系统负责实现上下文恢复，避免 Agent 运行时依赖业务监控模块。</p>
 */
public interface AgentToolTraceContextBridge {

    /**
     * 为当前 Tool 执行线程打开 Trace 上下文。
     *
     * @param context Agent 运行上下文
     * @return 执行结束时需要关闭的作用域
     */
    Scope open(AgentContext context);

    /**
     * Trace 上下文作用域。
     */
    @FunctionalInterface
    interface Scope extends AutoCloseable {

        /**
         * 空作用域。
         */
        Scope NOOP = () -> {
        };

        /**
         * 关闭作用域并恢复执行前的线程上下文。
         */
        @Override
        void close();
    }
}
