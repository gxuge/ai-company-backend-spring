package org.jeecg.modules.openapi.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.util.PromptRenderUtil;
import org.jeecg.modules.openapi.vo.PromptTemplateCodeVersionVo;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Prompt 模板渲染服务实现。
 */
@Slf4j
@Service
public class PromptRenderServiceImpl implements PromptRenderService {
    @Resource
    private IAiragPromptTemplateService airagPromptTemplateService;

    /**
     * 按模板路径与变量渲染最终 prompt，并输出模板替换日志。
     */
    @Override
    public String renderPrompt(String templatePath, Map<String, String> variables) {
        String normalizedPath = PromptRenderUtil.normalizeTemplatePath(templatePath);
        PromptTemplateCodeVersionVo codeVersion = PromptRenderUtil.parseCodeVersion(normalizedPath);
        Map<String, String> safeVariables = PromptRenderUtil.safeVariables(variables);

        AiragPromptTemplateVo template = airagPromptTemplateService.getTemplate(codeVersion.getCode(), codeVersion.getVersion());
        String developerPrompt = PromptRenderUtil.trimToEmpty(template.getSections().get("developer_prompt"));
        String userPrompt = PromptRenderUtil.trimToEmpty(
                airagPromptTemplateService.renderSection(codeVersion.getCode(), codeVersion.getVersion(), "user_prompt_template", safeVariables)
        );
        String outputSchemaHint = PromptRenderUtil.trimToEmpty(template.getSections().get("output_schema_hint"));
        String renderedPrompt = PromptRenderUtil.buildFinalPrompt(
                developerPrompt,
                PromptRenderUtil.replaceUnfilledPlaceholders(userPrompt, "null"),
                outputSchemaHint
        );

        String logContent = renderedPrompt.length() > 300 ? renderedPrompt.substring(0, 300) + "...(truncated)" : renderedPrompt;
        log.info("Prompt模板替换完成 path={}, vars={}, 替换后内容={}", templatePath, safeVariables, logContent);
        return renderedPrompt;
    }
}
