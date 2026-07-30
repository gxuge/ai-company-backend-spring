package org.jeecg.modules.system.util;

import org.jeecg.modules.openapi.util.PromptRenderUtil;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.springframework.util.StringUtils;

/**
 * TS 用户可见内容的提示词语言注入器。
 */
public final class TsPromptLanguageInjector {
    private static final String RESPONSE_LANGUAGE_PLACEHOLDER = "{{response_language}}";
    private static final String LANGUAGE_INSTRUCTION_TEMPLATE = """
            所有面向用户的文本内容必须使用{{response_language}}回复。
            JSON字段名、枚举值、Schema字段和技术标识保持原样。
            """;
    private static final String LANGUAGE_INSTRUCTION_PREFIX = "所有面向用户的文本内容必须使用";

    private TsPromptLanguageInjector() {
    }

    /**
     * 将当前请求语言约束追加到 Developer Prompt。
     */
    public static PromptRenderedSectionsVo inject(PromptRenderedSectionsVo sections) {
        if (sections == null) {
            return null;
        }
        String developerPrompt = appendInstruction(
                sections.getDeveloperPrompt(),
                TsResponseLanguageSupport.currentLanguageName()
        );
        String outputSpec = StringUtils.hasText(sections.getOutputSchemaHint())
                ? sections.getOutputSchemaHint()
                : sections.getToolSchema();
        sections.setDeveloperPrompt(developerPrompt);
        sections.setRenderedPrompt(PromptRenderUtil.buildFinalPrompt(
                developerPrompt,
                sections.getUserPrompt(),
                outputSpec
        ));
        return sections;
    }

    /**
     * 将当前请求语言约束追加到普通文本提示词。
     */
    public static String inject(String prompt) {
        return appendInstruction(prompt, TsResponseLanguageSupport.currentLanguageName());
    }

    private static String appendInstruction(String prompt, String responseLanguage) {
        String source = prompt == null ? "" : prompt.trim();
        if (source.contains(LANGUAGE_INSTRUCTION_PREFIX)) {
            return source;
        }
        String instruction = LANGUAGE_INSTRUCTION_TEMPLATE
                .replace(RESPONSE_LANGUAGE_PLACEHOLDER, responseLanguage)
                .trim();
        return StringUtils.hasText(source) ? source + "\n\n" + instruction : instruction;
    }

}
