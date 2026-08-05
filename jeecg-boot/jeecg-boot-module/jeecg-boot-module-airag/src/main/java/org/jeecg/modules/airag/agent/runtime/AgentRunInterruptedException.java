package org.jeecg.modules.airag.agent.runtime;

/**
 * Signals that the user explicitly stopped the current Agent run.
 */
public class AgentRunInterruptedException extends RuntimeException {

    public AgentRunInterruptedException() {
        super("Agent run interrupted by user");
    }
}
