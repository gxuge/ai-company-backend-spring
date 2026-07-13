package org.jeecg.modules.airag.agent.trace;

/**
 * Extension point for persisting Agent LLM request and response traces.
 */
public interface AgentLlmTraceSink {

    /**
     * Persist LLM request trace.
     *
     * @param request request trace payload
     */
    void onRequest(AgentLlmTraceRequest request);

    /**
     * Persist LLM response trace.
     *
     * @param response response trace payload
     */
    void onResponse(AgentLlmTraceResponse response);
}
