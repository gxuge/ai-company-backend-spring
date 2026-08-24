package org.jeecg.modules.system.util.tsad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 广告投放规则JSON转换与匹配工具。 */
public final class TsAdRuleUtils {

    /** 工具类不允许实例化。 */
    private TsAdRuleUtils() {
    }

    /** 将字符串列表序列化为JSON。 */
    public static String writeList(ObjectMapper objectMapper, List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("广告投放规则序列化失败", exception);
        }
    }

    /** 解析字符串数组JSON；空值按默认列表返回，非法JSON返回空列表。 */
    public static List<String> readList(
            ObjectMapper objectMapper, String json, List<String> defaultValues) {
        if (!StringUtils.hasText(json)) {
            return defaultValues;
        }
        try {
            List<String> values = objectMapper.readValue(
                    json, new TypeReference<List<String>>() {
                    });
            return values == null ? Collections.emptyList() : values;
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    /** 判断规则列表是否允许给定取值。 */
    public static boolean allows(List<String> values, String actual) {
        if (values == null || values.isEmpty() || !StringUtils.hasText(actual)) {
            return false;
        }
        String normalized = actual.trim().toUpperCase(Locale.ROOT);
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .anyMatch(value -> "ALL".equals(value) || normalized.equals(value));
    }
}
