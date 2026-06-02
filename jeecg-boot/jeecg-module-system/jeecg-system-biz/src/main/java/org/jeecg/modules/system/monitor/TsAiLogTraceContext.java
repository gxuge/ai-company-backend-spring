package org.jeecg.modules.system.monitor;

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
        state.stepNo++;
        return state.stepNo;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class State {
        private final Long logId;
        private final String traceId;
        private int stepNo;

        public State(Long logId, String traceId) {
            this.logId = logId;
            this.traceId = traceId;
            this.stepNo = 0;
        }

        public Long getLogId() {
            return logId;
        }

        public String getTraceId() {
            return traceId;
        }
    }
}
