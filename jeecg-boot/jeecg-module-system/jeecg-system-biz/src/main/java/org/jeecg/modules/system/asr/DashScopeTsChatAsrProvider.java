package org.jeecg.modules.system.asr;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.config.TsChatAsrConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streams PCM audio to Alibaba Cloud Model Studio.
 */
@Component
public class DashScopeTsChatAsrProvider implements TsChatAsrProvider {

    private final TsChatAsrConfig config;

    public DashScopeTsChatAsrProvider(TsChatAsrConfig config) {
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }

    @Override
    public TsChatAsrSession open(TsChatAsrListener listener) {
        validateConfig();
        Constants.baseWebsocketApiUrl = config.getWebsocketApiUrl();

        Recognition recognition = new Recognition();
        var builder = RecognitionParam.builder()
                .model(config.getModel())
                .apiKey(config.getApiKey())
                .format(config.getFormat())
                .sampleRate(config.getSampleRate())
                .parameter("max_sentence_silence", config.getMaxSentenceSilenceMs());
        if (StringUtils.hasText(config.getLanguage())) {
            builder.parameter("language_hints", new String[]{config.getLanguage()});
        }
        recognition.call(builder.build(), new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                if (result.getSentence() != null && StringUtils.hasText(result.getSentence().getText())) {
                    listener.onTranscript(result.getSentence().getText(), result.isSentenceEnd());
                }
            }

            @Override
            public void onComplete() {
                listener.onComplete();
            }

            @Override
            public void onError(Exception exception) {
                listener.onError(exception.getMessage());
            }
        });

        return new DashScopeSession(recognition, listener);
    }

    private void validateConfig() {
        if (!config.isEnabled()) {
            throw new JeecgBootException("ASR service is disabled");
        }
        if (!StringUtils.hasText(config.getApiKey()) || !StringUtils.hasText(config.getWorkspaceId())) {
            throw new JeecgBootException("ASR service is not configured");
        }
    }

    private static class DashScopeSession implements TsChatAsrSession {

        private final Recognition recognition;
        private final TsChatAsrListener listener;
        private final AtomicBoolean closed = new AtomicBoolean();

        private DashScopeSession(Recognition recognition, TsChatAsrListener listener) {
            this.recognition = recognition;
            this.listener = listener;
        }

        @Override
        public void sendAudio(byte[] audio) {
            if (!closed.get() && audio.length > 0) {
                recognition.sendAudioFrame(ByteBuffer.wrap(audio));
            }
        }

        @Override
        public void finish() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    recognition.stop();
                } catch (Exception exception) {
                    listener.onError(exception.getMessage());
                } finally {
                    recognition.getDuplexApi().close(1000, "completed");
                }
            });
        }

        @Override
        public void cancel() {
            if (closed.compareAndSet(false, true)) {
                recognition.getDuplexApi().close(1000, "cancelled");
            }
        }
    }
}
