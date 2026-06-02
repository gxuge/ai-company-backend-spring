package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.system.monitor.TsAiLogCollector;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified toolcall JSON repair pipeline shared by ts_* generation interfaces.
 */
@Slf4j
@Service
public class ToolcallJsonRepairService {
    private static final String METADATA_TOOLCALL_REPAIR_PROMPT_KEY = "toolcallJsonRepairPromptTemplate";
    private static final String METADATA_JSON_REPAIR_PROMPT_KEY = "jsonRepairPromptTemplate";
    private static final String METADATA_STORY_REPAIR_PROMPT_KEY = "storyJsonRepairPromptTemplate";
    private static final String DEFAULT_REPAIR_PROMPT_CODE = "toolcall_json_repair";
    private static final String DEFAULT_REPAIR_PROMPT_VERSION = "v2";

    @Resource
    private IPromptChatService promptChatService;
    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource
    private AiragAppMapper airagAppMapper;
    @Resource
    private TsAiLogCollector tsAiLogCollector;

    public JSONObject chatToolCallWithSchemaRepair(PromptRenderedSectionsVo sections, String scene) {
        List<String> requiredFields = extractRequiredFields(sections.getToolSchema());
        log.info("[PROMPT_CHAT_JSON_FULL] stage=schema-summary scene={} promptCode={} promptVersion={} requiredFields={}",
                scene, sections.getCode(), sections.getVersion(), requiredFields);
        tsAiLogCollector.appendStep("schema_summary", "Schema摘要", "success", step -> {
            step.setPromptCode(sections.getCode());
            step.setPromptVersion(sections.getVersion());
            step.setToolSchema(sections.getToolSchema());
            step.setValidationIssues(String.join(", ", requiredFields));
            step.setExtraInfoJson(buildSchemaSummaryJson(scene, requiredFields, extractRequiredFieldHints(sections.getToolSchema())));
        });

        String rawContent = null;
        try {
            rawContent = promptChatService.chatToolCall(
                    sections.getDeveloperPrompt(),
                    sections.getUserPrompt(),
                    sections.getToolSchema());
            JSONObject parsed = PromptRuntimeUtil.parseJsonObject(rawContent);
            List<String> firstPassIssues = validateAgainstToolSchema(parsed, sections.getToolSchema());
            if (firstPassIssues.isEmpty()) {
                log.info("[PROMPT_CHAT_JSON_FULL] stage=first-pass payload={}",
                        PromptRuntimeUtil.sanitizeToolCallLogJson(parsed).toJSONString());
                final String firstPassRawContent = rawContent;
                final String firstPassJson = PromptRuntimeUtil.sanitizeToolCallLogJson(parsed).toJSONString();
                tsAiLogCollector.appendStep("first_pass_result", "首轮结果", "success", step -> {
                    step.setPromptCode(sections.getCode());
                    step.setPromptVersion(sections.getVersion());
                    step.setResponseRaw(PromptRuntimeUtil.trimToNull(firstPassRawContent));
                    step.setResponseJson(firstPassJson);
                    step.setFinalOutputJson(firstPassJson);
                });
                return parsed;
            }
            log.warn("[PROMPT_CHAT_JSON_FULL] stage=first-pass-schema-mismatch scene={} issues={} payload={}",
                    scene, firstPassIssues, PromptRuntimeUtil.sanitizeToolCallLogJson(parsed).toJSONString());
            final String firstPassRawContent = rawContent;
            final String firstPassJson = PromptRuntimeUtil.sanitizeToolCallLogJson(parsed).toJSONString();
            tsAiLogCollector.appendStep("first_pass_result", "首轮结果", "failed", step -> {
                step.setPromptCode(sections.getCode());
                step.setPromptVersion(sections.getVersion());
                step.setResponseRaw(PromptRuntimeUtil.trimToNull(firstPassRawContent));
                step.setResponseJson(firstPassJson);
                step.setValidationIssues(String.join("; ", firstPassIssues));
            });
        } catch (Exception firstEx) {
            log.warn("[PROMPT_CHAT_JSON_FULL] stage=first-pass-parse-fail scene={} reason={}", scene, firstEx.getMessage());
            final String firstPassRawContent = rawContent;
            tsAiLogCollector.appendStep("first_pass_result", "首轮结果", "failed", step -> {
                step.setPromptCode(sections.getCode());
                step.setPromptVersion(sections.getVersion());
                step.setResponseRaw(PromptRuntimeUtil.trimToNull(firstPassRawContent));
                step.setValidationIssues(trimToNull(firstEx.getMessage()));
            });
        }

        PromptTemplateRef repairTemplateRef = resolveJsonRepairTemplateRef();
        tsAiLogCollector.markRepairTemplate(repairTemplateRef.code(), repairTemplateRef.version());
        log.info("[PROMPT_CHAT_JSON_FULL] stage=repair-plan scene={} firstPromptCode={} firstPromptVersion={} requiredFields={} repairPromptCode={} repairPromptVersion={}",
                scene, sections.getCode(), sections.getVersion(), requiredFields, repairTemplateRef.code(), repairTemplateRef.version());
        tsAiLogCollector.appendStep("repair_plan", "修复计划", "success", step -> {
            step.setPromptCode(repairTemplateRef.code());
            step.setPromptVersion(repairTemplateRef.version());
            step.setValidationIssues(String.join(", ", requiredFields));
            step.setExtraInfoJson(buildSchemaSummaryJson(scene, requiredFields, extractRequiredFieldHints(sections.getToolSchema())));
        });
        PromptRenderedSectionsVo repairPrompt = promptRenderService.renderPromptSections(
                repairTemplateRef.code(), repairTemplateRef.version(),
                buildJsonRepairVars(scene, rawContent, sections.getToolSchema()));
        log.info("[PROMPT_CHAT_JSON_FULL] stage=repair-template scene={} promptCode={} promptVersion={}",
                scene, repairPrompt.getCode(), repairPrompt.getVersion());

        String repairedContent = promptChatService.chatToolCall(
                repairPrompt.getDeveloperPrompt(),
                repairPrompt.getUserPrompt(),
                sections.getToolSchema());
        JSONObject repairedJson;
        try {
            repairedJson = PromptRuntimeUtil.parseJsonObject(repairedContent);
        } catch (Exception ex) {
            log.error("[PROMPT_CHAT_JSON_FULL] stage=repair-pass-parse-fail scene={} firstLen={} repairedLen={}",
                    scene,
                    rawContent == null ? 0 : rawContent.length(),
                    repairedContent == null ? 0 : repairedContent.length());
            throw new JeecgBootException("AI回复解析失败，非有效JSON");
        }
        List<String> repairIssues = validateAgainstToolSchema(repairedJson, sections.getToolSchema());
        if (!repairIssues.isEmpty()) {
            tsAiLogCollector.appendStep("repair_result", "修复结果", "failed", step -> {
                step.setPromptCode(repairPrompt.getCode());
                step.setPromptVersion(repairPrompt.getVersion());
                step.setResponseRaw(PromptRuntimeUtil.trimToNull(repairedContent));
                step.setResponseJson(PromptRuntimeUtil.sanitizeToolCallLogJson(repairedJson).toJSONString());
                step.setValidationIssues(String.join("; ", repairIssues));
            });
            throw new JeecgBootException("AI回复不满足tool schema约束: " + String.join("; ", repairIssues));
        }
        log.info("[PROMPT_CHAT_JSON_FULL] stage=repair-pass payload={}",
                PromptRuntimeUtil.sanitizeToolCallLogJson(repairedJson).toJSONString());
        tsAiLogCollector.appendStep("repair_result", "修复结果", "success", step -> {
            step.setPromptCode(repairPrompt.getCode());
            step.setPromptVersion(repairPrompt.getVersion());
            step.setResponseRaw(PromptRuntimeUtil.trimToNull(repairedContent));
            step.setResponseJson(PromptRuntimeUtil.sanitizeToolCallLogJson(repairedJson).toJSONString());
            step.setFinalOutputJson(PromptRuntimeUtil.sanitizeToolCallLogJson(repairedJson).toJSONString());
        });
        return repairedJson;
    }

    private Map<String, String> buildJsonRepairVars(String scene, String rawContent, String toolSchema) {
        Map<String, String> variables = new HashMap<>();
        variables.put("scene", PromptRuntimeUtil.nullableToken(scene));
        variables.put("raw_content", PromptRuntimeUtil.nullableToken(PromptRuntimeUtil.trimToNull(rawContent)));
        variables.put("tool_schema", PromptRuntimeUtil.nullableToken(PromptRuntimeUtil.trimToNull(toolSchema)));
        variables.put("required_fields", PromptRuntimeUtil.nullableToken(String.join(", ", extractRequiredFields(toolSchema))));
        variables.put("required_field_hints", PromptRuntimeUtil.nullableToken(extractRequiredFieldHints(toolSchema)));
        return variables;
    }

    private List<String> validateAgainstToolSchema(JSONObject data, String toolSchema) {
        List<String> issues = new ArrayList<>();
        if (data == null) {
            issues.add("response is null");
            return issues;
        }
        JSONObject schemaRoot;
        try {
            schemaRoot = StringUtils.hasText(toolSchema) ? JSONObject.parseObject(toolSchema) : null;
        } catch (Exception ex) {
            issues.add("tool_schema parse failed: " + ex.getMessage());
            return issues;
        }
        JSONObject parameters = schemaRoot == null ? null : schemaRoot.getJSONObject("parameters");
        JSONObject properties = parameters == null ? null : parameters.getJSONObject("properties");
        List<String> requiredFields = extractRequiredFields(toolSchema);
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                issues.add("missing required field: " + field);
                continue;
            }
            Object value = data.get(field);
            if (value == null) {
                issues.add("required field is null: " + field);
                continue;
            }
            if (value instanceof String str && !StringUtils.hasText(str)) {
                issues.add("required field is blank: " + field);
            }
        }
        if (properties == null || properties.isEmpty()) {
            return issues;
        }
        for (String field : properties.keySet()) {
            if (!data.containsKey(field)) {
                continue;
            }
            JSONObject fieldSchema = properties.getJSONObject(field);
            if (fieldSchema == null) {
                continue;
            }
            Object value = data.get(field);
            String type = trimToNull(fieldSchema.getString("type"));
            JSONArray enumArray = fieldSchema.getJSONArray("enum");

            if (StringUtils.hasText(type)) {
                boolean typeOk = switch (type) {
                    case "string" -> value instanceof String;
                    case "number" -> value instanceof Number;
                    case "integer" -> value instanceof Number && isIntegerValue((Number) value);
                    case "boolean" -> value instanceof Boolean;
                    case "array" -> value instanceof JSONArray || value instanceof List<?>;
                    case "object" -> value instanceof JSONObject || value instanceof Map<?, ?>;
                    default -> true;
                };
                if (!typeOk) {
                    issues.add("type mismatch field=" + field + ", expected=" + type + ", actual=" + value.getClass().getSimpleName());
                }
            }
            if (enumArray != null && !enumArray.isEmpty()) {
                boolean matched = false;
                for (Object enumItem : enumArray) {
                    if ((enumItem == null && value == null) || (enumItem != null && enumItem.equals(value))) {
                        matched = true;
                        break;
                    }
                    if (enumItem != null && value != null && String.valueOf(enumItem).equals(String.valueOf(value))) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    issues.add("enum mismatch field=" + field + ", value=" + value);
                }
            }
        }
        return issues;
    }

    private boolean isIntegerValue(Number value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return true;
        }
        double d = value.doubleValue();
        return Math.floor(d) == d;
    }

    private List<String> extractRequiredFields(String toolSchema) {
        List<String> requiredFields = new ArrayList<>();
        if (!StringUtils.hasText(toolSchema)) {
            return requiredFields;
        }
        try {
            JSONObject schemaRoot = JSONObject.parseObject(toolSchema);
            JSONObject parameters = schemaRoot == null ? null : schemaRoot.getJSONObject("parameters");
            JSONArray required = parameters == null ? null : parameters.getJSONArray("required");
            if (required == null || required.isEmpty()) {
                return requiredFields;
            }
            for (Object item : required) {
                if (item == null) {
                    continue;
                }
                String key = PromptRuntimeUtil.trimToNull(String.valueOf(item));
                if (StringUtils.hasText(key)) {
                    requiredFields.add(key);
                }
            }
            return requiredFields;
        } catch (Exception ex) {
            log.warn("Failed to extract required fields from tool_schema, reason={}", ex.getMessage());
            return requiredFields;
        }
    }

    private String extractRequiredFieldHints(String toolSchema) {
        List<String> hints = new ArrayList<>();
        if (!StringUtils.hasText(toolSchema)) {
            return "";
        }
        try {
            JSONObject schemaRoot = JSONObject.parseObject(toolSchema);
            JSONObject parameters = schemaRoot == null ? null : schemaRoot.getJSONObject("parameters");
            JSONObject properties = parameters == null ? null : parameters.getJSONObject("properties");
            List<String> requiredFields = extractRequiredFields(toolSchema);
            for (String field : requiredFields) {
                JSONObject fieldSchema = properties == null ? null : properties.getJSONObject(field);
                String description = fieldSchema == null ? null : trimToNull(fieldSchema.getString("description"));
                hints.add(StringUtils.hasText(description) ? field + ": " + description : field);
            }
            return String.join("；", hints);
        } catch (Exception ex) {
            log.warn("Failed to extract required field hints from tool_schema, reason={}", ex.getMessage());
            return "";
        }
    }

    private PromptTemplateRef resolveJsonRepairTemplateRef() {
        AiragApp app = resolvePromptApp();
        if (!StringUtils.hasText(app.getMetadata())) {
            return new PromptTemplateRef(DEFAULT_REPAIR_PROMPT_CODE, DEFAULT_REPAIR_PROMPT_VERSION);
        }
        try {
            JSONObject metadata = JSONObject.parseObject(app.getMetadata());
            if (metadata == null) {
                return new PromptTemplateRef(DEFAULT_REPAIR_PROMPT_CODE, DEFAULT_REPAIR_PROMPT_VERSION);
            }

            PromptTemplateRef ref = firstNonNull(
                    parseTemplateRef(metadata.get(METADATA_TOOLCALL_REPAIR_PROMPT_KEY)),
                    parseTemplateRef(metadata.get(METADATA_JSON_REPAIR_PROMPT_KEY)),
                    parseTemplateRef(metadata.get(METADATA_STORY_REPAIR_PROMPT_KEY)));

            if (ref == null || !StringUtils.hasText(ref.code()) || !StringUtils.hasText(ref.version())) {
                return new PromptTemplateRef(DEFAULT_REPAIR_PROMPT_CODE, DEFAULT_REPAIR_PROMPT_VERSION);
            }
            return ref;
        } catch (Exception ex) {
            log.warn("解析JSON修复模板配置失败，fallback默认模板，reason={}", ex.getMessage());
            return new PromptTemplateRef(DEFAULT_REPAIR_PROMPT_CODE, DEFAULT_REPAIR_PROMPT_VERSION);
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private PromptTemplateRef parseTemplateRef(Object rawConfig) {
        if (rawConfig == null) {
            return null;
        }
        if (rawConfig instanceof JSONObject jsonObject) {
            String configuredCode = trimToNull(jsonObject.getString("code"));
            String configuredVersion = trimToNull(jsonObject.getString("version"));
            if (!StringUtils.hasText(configuredCode) && !StringUtils.hasText(configuredVersion)) {
                return null;
            }
            return new PromptTemplateRef(configuredCode, configuredVersion);
        }
        String text = trimToNull(String.valueOf(rawConfig));
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("@")) {
            String[] parts = text.split("@", 2);
            String configuredCode = trimToNull(parts[0]);
            String configuredVersion = parts.length > 1 ? trimToNull(parts[1]) : null;
            return new PromptTemplateRef(configuredCode, configuredVersion);
        }
        return new PromptTemplateRef(text, null);
    }

    private AiragApp resolvePromptApp() {
        String appId = trimToNull(promptChatConfigBean.getAppId());
        if (!StringUtils.hasText(appId)) {
            throw new JeecgBootBizTipException("未配置 jeecg.airag.prompt-chat.app-id，无法解析JSON修复模板");
        }
        AiragApp app = airagAppMapper.getByIdIgnoreTenant(appId);
        if (app == null) {
            throw new JeecgBootBizTipException("未找到AI应用配置，appId=" + appId);
        }
        return app;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildSchemaSummaryJson(String scene, List<String> requiredFields, String requiredFieldHints) {
        JSONObject info = new JSONObject();
        info.put("scene", trimToNull(scene));
        info.put("requiredFields", requiredFields);
        info.put("requiredFieldHints", trimToNull(requiredFieldHints));
        return info.toJSONString();
    }

    private record PromptTemplateRef(String code, String version) {
    }
}
