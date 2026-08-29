package org.jeecg.modules.system.asr;

/**
 * Receives streaming speech recognition events.
 */
public interface TsChatAsrListener {

    void onTranscript(String text, boolean sentenceEnd);

    void onComplete();

    void onError(String message);
}
