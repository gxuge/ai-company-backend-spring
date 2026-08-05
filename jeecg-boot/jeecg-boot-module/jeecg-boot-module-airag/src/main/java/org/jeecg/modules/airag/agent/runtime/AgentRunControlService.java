package org.jeecg.modules.airag.agent.runtime;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controls active Agent runs by runId.
 */
@Service
public class AgentRunControlService {

    private static final String ATTR_STOP_SIGNAL = "agentRunStopSignal";

    private final Map<String, RunState> activeRuns = new ConcurrentHashMap<>();

    public void register(AgentContext context) {
        if (context == null) {
            return;
        }
        context.normalize();
        StopSignal signal = new StopSignal();
        context.putAttribute(ATTR_STOP_SIGNAL, signal);
        this.activeRuns.put(context.getRunId(), new RunState(
                context.getSessionId(),
                context.getUserId(),
                signal
        ));
    }

    public void bindCurrentThread(AgentContext context) {
        RunState state = find(context);
        if (state != null) {
            state.thread = Thread.currentThread();
        }
        throwIfStopRequested(context);
    }

    public StopResult requestStop(String runId, Long sessionId, String userId) {
        RunState state = this.activeRuns.get(normalize(runId));
        if (state == null) {
            return StopResult.NOT_FOUND;
        }
        if (sessionId != null && !sessionId.equals(state.sessionId)) {
            return StopResult.NOT_FOUND;
        }
        if (userId != null && !userId.equals(state.userId)) {
            return StopResult.NOT_FOUND;
        }
        if (!state.signal.requested.compareAndSet(false, true)) {
            return StopResult.ALREADY_STOPPED;
        }
        Thread thread = state.thread;
        if (thread != null) {
            thread.interrupt();
        }
        return StopResult.STOP_REQUESTED;
    }

    public void unregister(AgentContext context) {
        if (context == null || context.getRunId() == null) {
            return;
        }
        this.activeRuns.remove(context.getRunId());
    }

    public static boolean isStopRequested(AgentContext context) {
        Object rawSignal = context == null ? null : context.getAttribute(ATTR_STOP_SIGNAL);
        return rawSignal instanceof StopSignal signal && signal.requested.get();
    }

    public static void throwIfStopRequested(AgentContext context) {
        if (isStopRequested(context)) {
            throw new AgentRunInterruptedException();
        }
    }

    public static boolean isInterrupted(Throwable error, AgentContext context) {
        if (isStopRequested(context)) {
            return true;
        }
        Throwable current = error;
        while (current != null) {
            if (current instanceof AgentRunInterruptedException
                    || current instanceof InterruptedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private RunState find(AgentContext context) {
        if (context == null || context.getRunId() == null) {
            return null;
        }
        return this.activeRuns.get(context.getRunId());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public enum StopResult {
        STOP_REQUESTED,
        ALREADY_STOPPED,
        NOT_FOUND
    }

    private static final class StopSignal {
        private final AtomicBoolean requested = new AtomicBoolean(false);
    }

    private static final class RunState {
        private final Long sessionId;
        private final String userId;
        private final StopSignal signal;
        private volatile Thread thread;

        private RunState(Long sessionId,
                         String userId,
                         StopSignal signal) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.signal = signal;
        }
    }
}
