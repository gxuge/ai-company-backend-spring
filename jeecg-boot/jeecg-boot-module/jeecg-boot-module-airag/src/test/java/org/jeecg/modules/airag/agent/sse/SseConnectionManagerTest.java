package org.jeecg.modules.airag.agent.sse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseConnectionManagerTest {

    @Test
    void shouldKeepConnectionUntilAsyncTaskFinishes() {
        SseConnectionManager manager = new SseConnectionManager();
        SseEmitter emitter = Mockito.mock(SseEmitter.class);

        manager.register("connection-1", emitter);
        manager.retain("connection-1");
        manager.finishRun("connection-1");

        Mockito.verify(emitter, Mockito.never()).complete();

        manager.release("connection-1");

        Mockito.verify(emitter).complete();
    }
}
