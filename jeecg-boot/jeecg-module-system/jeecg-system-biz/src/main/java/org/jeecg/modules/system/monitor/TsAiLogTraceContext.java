package org.jeecg.modules.system.monitor;

import java.util.concurrent.atomic.AtomicInteger;

public class TsAiLogTraceContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    private TsAiLogTraceContext() {
    }

    public static void set(State state) {
        HOLDER.set(state);
    }

    public static State get() {
        return HOLDER.get();
    }

    public static boolean isActive() {
        return HOLDER.get() != null;
    }

    public static int nextStepNo() {
        State state = HOLDER.get();
        if (state == null) {
            return 0;
        }
        return state.nextStepNo();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class State {
        private final Long logId;
        private final String traceId;
        private final AtomicInteger stepCounter;

        public State(Long logId, String traceId) {
            this(logId, traceId, new AtomicInteger(0));
        }

        public State(Long logId, String traceId, AtomicInteger stepCounter) {
            this.logId = logId;
            this.traceId = traceId;
            this.stepCounter = stepCounter == null ? new AtomicInteger(0) : stepCounter;
        }

        public Long getLogId() {
            return logId;
        }

        public String getTraceId() {
            return traceId;
        }

        private int nextStepNo() {
            return this.stepCounter.incrementAndGet();
        }
    }
}
