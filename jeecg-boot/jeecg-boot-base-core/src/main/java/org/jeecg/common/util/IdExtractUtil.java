package org.jeecg.common.util;

import java.lang.reflect.Method;

/**
 * 参数ID提取工具。
 * 功能：从方法参数中统一提取 Long 类型资源ID。
 */
public final class IdExtractUtil {

    private IdExtractUtil() {
    }

    /**
     * 从参数数组中提取第一个可转换为 Long 的值。
     */
    public static Long extractLongId(Object[] args) {
        return extractLongId(args, (String[]) null);
    }

    /**
     * 从参数数组中提取第一个可转换为 Long 的值。
     * 先尝试参数本身，再按 getter 名提取（如 getStoryId）。
     */
    public static Long extractLongId(Object[] args, String... getterNames) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            Long value = toLong(arg);
            if (value != null) {
                return value;
            }
            if (getterNames != null) {
                for (String getterName : getterNames) {
                    value = toLong(invokeGetter(arg, getterName));
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将对象转换为 Long。
     * 支持 Long/Integer/Number/String。
     */
    public static Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Object invokeGetter(Object target, String getterName) {
        if (target == null || getterName == null || getterName.trim().isEmpty()) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(getterName);
            return method.invoke(target);
        } catch (Exception ex) {
            return null;
        }
    }
}
