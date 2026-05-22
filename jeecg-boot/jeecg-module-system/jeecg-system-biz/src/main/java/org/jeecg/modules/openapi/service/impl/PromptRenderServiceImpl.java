package org.jeecg.modules.openapi.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.util.PromptRenderUtil;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.openapi.vo.PromptTemplateCodeVersionVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Prompt template rendering service implementation.
 */
@Slf4j
@Service
public class PromptRenderServiceImpl implements PromptRenderService {
    @Resource
    private IAiragPromptTemplateService airagPromptTemplateService;

    @Override
    public String renderPrompt(String templatePath, Map<String, String> variables) {
        return renderPromptSections(templatePath, variables).getRenderedPrompt();
    }

    @Override
    public PromptRenderedSectionsVo renderPromptSections(String templatePath, Map<String, String> variables) {
        String normalizedPath = PromptRenderUtil.normalizeTemplatePath(templatePath);
        PromptTemplateCodeVersionVo codeVersion = PromptRenderUtil.parseCodeVersion(normalizedPath);
        Map<String, String> safeVariables = PromptRenderUtil.safeVariables(variables);

        AiragPromptTemplateVo template = airagPromptTemplateService.getTemplate(codeVersion.getCode(), codeVersion.getVersion());
        String developerPrompt = PromptRenderUtil.trimToEmpty(template.getSections().get("developer_prompt"));
        String userPromptRaw = PromptRenderUtil.trimToEmpty(
                airagPromptTemplateService.renderSection(codeVersion.getCode(), codeVersion.getVersion(), "user_prompt_template", safeVariables)
        );
        String userPrompt = PromptRenderUtil.replaceUnfilledPlaceholders(userPromptRaw, "null");
        String outputSchemaHint = PromptRenderUtil.trimToEmpty(template.getSections().get("output_schema_hint"));
        String toolSchema = PromptRenderUtil.trimToEmpty(template.getSections().get("tool_schema"));
        String outputSpec = StringUtils.hasText(outputSchemaHint) ? outputSchemaHint : toolSchema;
        String renderedPrompt = PromptRenderUtil.buildFinalPrompt(developerPrompt, userPrompt, outputSpec);

        PromptRenderedSectionsVo sections = new PromptRenderedSectionsVo();
        sections.setTemplatePath(templatePath);
        sections.setCode(codeVersion.getCode());
        sections.setVersion(codeVersion.getVersion());
        sections.setDeveloperPrompt(developerPrompt);
        sections.setUserPrompt(userPrompt);
        sections.setOutputSchemaHint(outputSchemaHint);
        sections.setToolSchema(toolSchema);
        sections.setRenderedPrompt(renderedPrompt);

        String logContent = renderedPrompt.length() > 300 ? renderedPrompt.substring(0, 300) + "...(truncated)" : renderedPrompt;
        log.info("Prompt template rendered path={}, vars={}, rendered={}", templatePath, safeVariables, logContent);
        return sections;
    }
}

