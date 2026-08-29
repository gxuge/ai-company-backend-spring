package org.jeecg.modules.system.asr;

/**
 * Represents one provider-side streaming recognition session.
 */
public interface TsChatAsrSession {

    void sendAudio(byte[] audio);

    void finish();

    void cancel();
}
