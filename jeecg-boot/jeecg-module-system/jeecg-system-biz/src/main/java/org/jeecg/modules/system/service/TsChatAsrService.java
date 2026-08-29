package org.jeecg.modules.system.service;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.asr.TsChatAsrListener;
import org.jeecg.modules.system.asr.TsChatAsrProvider;
import org.jeecg.modules.system.asr.TsChatAsrSession;
import org.jeecg.modules.system.config.TsChatAsrConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Selects the configured speech recognition provider.
 */
@Service
public class TsChatAsrService {

    private final TsChatAsrConfig config;
    private final List<TsChatAsrProvider> providers;

    public TsChatAsrService(TsChatAsrConfig config, List<TsChatAsrProvider> providers) {
        this.config = config;
        this.providers = providers;
    }

    public TsChatAsrSession open(TsChatAsrListener listener) {
        return providers.stream()
                .filter(provider -> provider.getProviderName().equalsIgnoreCase(config.getProvider()))
                .findFirst()
                .orElseThrow(() -> new JeecgBootException("Unsupported ASR provider"))
                .open(listener);
    }
}
