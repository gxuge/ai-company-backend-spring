package org.jeecg.modules.airag.safety.moderation;

import java.util.Locale;

/**
 * 第一版文本审核类别。
 */
public enum ModerationCategory {
    NONE,
    SEXUAL,
    SEXUAL_MINOR,
    VIOLENCE,
    SELF_HARM,
    HATE,
    HARASSMENT,
    ILLEGAL,
    PRIVACY,
    UNKNOWN;

    /**
     * 将供应商类别转换为统一枚举。
     *
     * @param value 供应商类别
     * @return 统一类别
     */
    public static ModerationCategory fromProviderValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
