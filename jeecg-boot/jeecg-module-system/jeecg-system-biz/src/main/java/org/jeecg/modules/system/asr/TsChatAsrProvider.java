package org.jeecg.modules.system.asr;

/**
 * Opens provider-specific streaming speech recognition sessions.
 */
public interface TsChatAsrProvider {

    String getProviderName();

    TsChatAsrSession open(TsChatAsrListener listener);
}
