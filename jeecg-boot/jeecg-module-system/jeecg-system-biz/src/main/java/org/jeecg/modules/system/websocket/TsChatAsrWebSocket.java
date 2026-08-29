package org.jeecg.modules.system.websocket;

import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.system.asr.TsChatAsrListener;
import org.jeecg.modules.system.asr.TsChatAsrSession;
import org.jeecg.modules.system.service.TsChatAsrService;
import org.jeecg.modules.system.service.TsChatAsrTicketService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Relays browser PCM frames to the configured ASR provider.
 */
@Slf4j
@Component
@ServerEndpoint("/websocket/ts-chat-asr/{ticket}")
public class TsChatAsrWebSocket {

    private Session browserSession;
    private TsChatAsrSession asrSession;

    @OnOpen
    public void onOpen(Session session, @PathParam("ticket") String ticket) throws IOException {
        TsChatAsrTicketService ticketService = SpringContextUtils.getBean(TsChatAsrTicketService.class);
        if (ticketService.consume(ticket) == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "invalid ticket"));
            return;
        }

        browserSession = session;
        TsChatAsrService asrService = SpringContextUtils.getBean(TsChatAsrService.class);
        try {
            asrSession = asrService.open(new BrowserListener());
            sendEvent("ready", null, false);
        } catch (Exception exception) {
            sendEvent("error", safeMessage(exception), false);
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "asr unavailable"));
        }
    }

    @OnMessage
    public void onBinary(ByteBuffer audio) {
        if (asrSession == null) {
            return;
        }
        byte[] bytes = new byte[audio.remaining()];
        audio.get(bytes);
        asrSession.sendAudio(bytes);
    }

    @OnMessage
    public void onCommand(String command) {
        if (asrSession == null) {
            return;
        }
        if ("finish".equals(command)) {
            asrSession.finish();
        } else if ("cancel".equals(command)) {
            asrSession.cancel();
        }
    }

    @OnClose
    public void onClose() {
        if (asrSession != null) {
            asrSession.cancel();
        }
    }

    @OnError
    public void onError(Throwable throwable) {
        log.warn("Chat ASR WebSocket error: {}", throwable.getMessage());
        if (asrSession != null) {
            asrSession.cancel();
        }
    }

    private void sendEvent(String type, String text, boolean sentenceEnd) {
        Session session = browserSession;
        if (session == null || !session.isOpen()) {
            return;
        }
        JSONObject message = new JSONObject();
        message.put("type", type);
        if (text != null) {
            message.put("text", text);
        }
        if ("transcript".equals(type)) {
            message.put("sentenceEnd", sentenceEnd);
        }
        session.getAsyncRemote().sendText(message.toJSONString());
    }

    private String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "ASR service error" : throwable.getMessage();
    }

    private class BrowserListener implements TsChatAsrListener {

        @Override
        public void onTranscript(String text, boolean sentenceEnd) {
            sendEvent("transcript", text, sentenceEnd);
        }

        @Override
        public void onComplete() {
            sendEvent("complete", null, false);
        }

        @Override
        public void onError(String message) {
            sendEvent("error", message == null ? "ASR service error" : message, false);
        }
    }
}
