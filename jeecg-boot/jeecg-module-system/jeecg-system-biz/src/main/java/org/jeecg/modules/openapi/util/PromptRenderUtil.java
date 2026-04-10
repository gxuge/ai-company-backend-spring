package org.jeecg.modules.openapi.util;

import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.openapi.vo.PromptTemplateCodeVersionVo;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 渲染相关通用工具。
 */
public class PromptRenderUtil {
    /**
     * 模板文件名规则：{code}_{version}.txt。
     */
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^(.+)_((?:v|V)[\\w.-]+)\\.txt$");
    /**
     * 变量占位符规则：{{variable}}。
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{[^{}]+\\}\\}");

    private PromptRenderUtil() {
    }

    /**
     * 规范化模板路径（去空白并统一分隔符）。
     */
    public static String normalizeTemplatePath(String templatePath) {
        if (!StringUtils.hasText(templatePath)) {
            throw new JeecgBootBizTipException("templatePath cannot be empty");
        }
        return templatePath.trim().replace("\\", "/");
    }

    /**
     * 从模板路径解析 code 与 version。
     */
    public static PromptTemplateCodeVersionVo parseCodeVersion(String normalizedTemplatePath) {
        String fileName = normalizedTemplatePath;
        int slashIndex = fileName.lastIndexOf('/');
        if (slashIndex >= 0) {
            fileName = fileName.substring(slashIndex + 1);
        }
        Matcher matcher = FILE_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            throw new JeecgBootBizTipException("templatePath format is invalid: " + normalizedTemplatePath);
        }
        return new PromptTemplateCodeVersionVo(matcher.group(1), matcher.group(2));
    }

    /**
     * 空变量安全兜底，避免 null map 参与渲染。
     */
    public static Map<String, String> safeVariables(Map<String, String> variables) {
        return variables == null ? Collections.emptyMap() : variables;
    }

    /**
     * 将文本收敛为非 null 的 trim 后字符串。
     */
    public static String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    /**
     * 把未替换的 {{...}} 占位符统一替换为给定字符串。
     */
    public static String replaceUnfilledPlaceholders(String source, String replacement) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String toValue = replacement == null ? "" : replacement;
        return PLACEHOLDER_PATTERN.matcher(source).replaceAll(Matcher.quoteReplacement(toValue));
    }

    /**
     * 组装最终 prompt 文本：developer + user + schema hint。
     */
    public static String buildFinalPrompt(String developerPrompt, String userPrompt, String outputSchemaHint) {
        return trimToEmpty(developerPrompt) + "\n\n" + trimToEmpty(userPrompt) + "\n\n" + trimToEmpty(outputSchemaHint);
    }
}
