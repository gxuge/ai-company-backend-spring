package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 故事场景选项提示词。
 */
@Data
public class TsStorySceneOptionDto {
    /** 时间选项允许的 key。 */
    public static final Set<String> TIME_KEYS = Set.of("random", "day", "dusk", "night", "morning");
    /** 天气选项允许的 key。 */
    public static final Set<String> WEATHER_KEYS = Set.of("random", "sun", "rain", "snow", "fog");
    /** 气氛选项允许的 key。 */
    public static final Set<String> MOOD_KEYS = Set.of("random", "warm", "mystic", "dream", "oppress");
    /** 描述最大长度。 */
    private static final int DESCRIPTION_MAX_LENGTH = 300;

    /** 前端稳定选项 key。 */
    private String key;
    /** 供视觉模型参考的英文描述。 */
    private String description;

    /**
     * 规范化并校验场景选项。
     *
     * @param option      原始场景选项
     * @param allowedKeys 当前字段允许的 key
     * @return 合法选项；空值或非法 key 返回 null
     */
    public static TsStorySceneOptionDto normalize(
            TsStorySceneOptionDto option, Set<String> allowedKeys) {
        if (option == null || allowedKeys == null) {
            return null;
        }
        option.key = trimToNull(option.key);
        option.description = limitText(trimToNull(option.description), DESCRIPTION_MAX_LENGTH);
        if (!StringUtils.hasText(option.key) || !allowedKeys.contains(option.key)) {
            return null;
        }
        return option;
    }

    private static String limitText(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
