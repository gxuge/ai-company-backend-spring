package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickOutlineGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSettingGenerateDto;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.jeecg.modules.system.util.StoryGenerateSnapshotUtil;
import org.jeecg.modules.system.util.StoryPromptGenerateUtil;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineChapterVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSettingGenerateVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 故事生成服务实现。
 */
@Service
@Slf4j
public class TsStoryGenerateServiceImpl implements ITsStoryGenerateService {
    private static final String METADATA_STORY_PROMPT_KEY = "storyPromptTemplate";
    private static final String METADATA_STORY_PROMPTS_KEY = "storyPromptTemplates";
    private static final String METADATA_TOOLCALL_REPAIR_PROMPT_KEY = "toolcallJsonRepairPromptTemplate";
    private static final String METADATA_JSON_REPAIR_PROMPT_KEY = "jsonRepairPromptTemplate";
    private static final String METADATA_STORY_REPAIR_PROMPT_KEY = "storyJsonRepairPromptTemplate";
    private static final String TOOLCALL_REPAIR_PROMPT_DIR = "prompts/toolcall/";
    private static final String REDIS_SNAPSHOT_PREFIX = "ts:story:generate:snapshot:";
    private static final long REDIS_SNAPSHOT_TTL_HOURS = 72L;
    private static final String ENDPOINT_STORY_SETTING_GENERATE = "/sys/ts-stories/story-setting-generate";
    private static final String ENDPOINT_STORY_SCENE_GENERATE = "/sys/ts-stories/story--scene-generate";
    private static final String ENDPOINT_STORY_OUTLINE_GENERATE = "/sys/ts-stories/story--outline-generate";

    @Resource
    private IPromptChatService promptChatService;
    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource
    private AiragAppMapper airagAppMapper;

    /**
     * 生成故事设定（标题/简介/模式/设定/背景）。
     */
    @Override
    public TsStoryOneClickSettingGenerateVo generateStorySetting(LoginUser user, TsStoryOneClickSettingGenerateDto request) {
        TsStoryOneClickSettingGenerateDto dto = request == null ? new TsStoryOneClickSettingGenerateDto() : request;
        dto.normalize();
        PromptTemplateRef templateRef = resolvePromptTemplateRef(TemplateScene.SETTING);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(templateRef.templatePath(), StoryPromptGenerateUtil.buildSettingVars(dto));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson;
        boolean generated = true;
        String fallbackReason = null;
        try {
            modelJson = callPromptChatWithSchemaRepair(promptSections, "setting");
        } catch (Exception ex) {
            modelJson = new JSONObject();
            modelJson.put("fallback", true);
            fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            modelJson.put("fallbackReason", fallbackReason);
            generated = false;
        }
        logStoryLlmJson(ENDPOINT_STORY_SETTING_GENERATE, modelJson, generated, fallbackReason);

        String title = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("title")),
                dto.getTitle(),
                "Original Story " + System.currentTimeMillis());
        String storyIntro = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_intro")),
                dto.getStoryIntro());
        String storyMode = StoryPromptGenerateUtil.normalizeStoryMode(PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_mode")),
                dto.getStoryMode(),
                "chapter"));
        String storySetting = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_setting")),
                dto.getStorySetting());
        String storyBackground = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_background")),
                dto.getStoryBackground());

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "story-setting");
        snapshot.put("promptCode", templateRef.code());
        snapshot.put("promptVersion", templateRef.version());
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        JSONObject resultJson = new JSONObject();
        resultJson.put("title", title);
        resultJson.put("story_intro", storyIntro);
        resultJson.put("story_mode", storyMode);
        resultJson.put("story_setting", storySetting);
        resultJson.put("story_background", storyBackground);
        snapshot.put("result", resultJson);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "setting", user.getId(), snapshot);

        TsStoryOneClickSettingGenerateVo vo = new TsStoryOneClickSettingGenerateVo();
        vo.setTitle(title);
        vo.setStoryIntro(storyIntro);
        vo.setStoryMode(storyMode);
        vo.setStorySetting(storySetting);
        vo.setStoryBackground(storyBackground);
        vo.setGenerated(generated);
        vo.setFallbackReason(fallbackReason);
        vo.setPromptCode(templateRef.code());
        vo.setPromptVersion(templateRef.version());
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 生成场所设定（场景快照名/摘要/元素）。
     */
    @Override
    public TsStoryOneClickSceneGenerateVo generateStoryScene(LoginUser user, TsStoryOneClickSceneGenerateDto request) {
        TsStoryOneClickSceneGenerateDto dto = request == null ? new TsStoryOneClickSceneGenerateDto() : request;
        dto.normalize();
        PromptTemplateRef templateRef = resolvePromptTemplateRef(TemplateScene.SCENE);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(templateRef.templatePath(), StoryPromptGenerateUtil.buildSceneVars(dto));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson;
        boolean generated = true;
        String fallbackReason = null;
        try {
            modelJson = callPromptChatWithSchemaRepair(promptSections, "scene");
        } catch (Exception ex) {
            modelJson = new JSONObject();
            modelJson.put("fallback", true);
            fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            modelJson.put("fallbackReason", fallbackReason);
            generated = false;
        }
        logStoryLlmJson(ENDPOINT_STORY_SCENE_GENERATE, modelJson, generated, fallbackReason);

        String sceneNameSnapshot = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("scene_name_snapshot")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("scene_name")),
                dto.getSceneSetting(),
                "未命名场景");
        String sceneSummary = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("scene_summary")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("scene_desc")),
                "这是一个等待你继续完善的场景。");
        List<String> sceneElements = StoryPromptGenerateUtil.parseStringList(modelJson.get("scene_elements"));

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "scene-setting");
        snapshot.put("promptCode", templateRef.code());
        snapshot.put("promptVersion", templateRef.version());
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        JSONObject resultJson = new JSONObject();
        resultJson.put("scene_name_snapshot", sceneNameSnapshot);
        resultJson.put("scene_summary", sceneSummary);
        resultJson.put("scene_elements", sceneElements);
        snapshot.put("result", resultJson);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "scene", user.getId(), snapshot);

        TsStoryOneClickSceneGenerateVo vo = new TsStoryOneClickSceneGenerateVo();
        vo.setSceneNameSnapshot(sceneNameSnapshot);
        vo.setSceneSummary(sceneSummary);
        vo.setSceneElements(sceneElements);
        vo.setGenerated(generated);
        vo.setFallbackReason(fallbackReason);
        vo.setPromptCode(templateRef.code());
        vo.setPromptVersion(templateRef.version());
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 生成剧情大纲（章节数组）。
     */
    @Override
    public TsStoryOneClickOutlineGenerateVo generateStoryOutline(LoginUser user, TsStoryOneClickOutlineGenerateDto request) {
        TsStoryOneClickOutlineGenerateDto dto = request == null ? new TsStoryOneClickOutlineGenerateDto() : request;
        dto.normalize();
        PromptTemplateRef templateRef = resolvePromptTemplateRef(TemplateScene.OUTLINE);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(templateRef.templatePath(), StoryPromptGenerateUtil.buildOutlineVars(dto));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson;
        try {
            modelJson = callPromptChatWithSchemaRepair(promptSections, "outline");
            logStoryLlmJson(ENDPOINT_STORY_OUTLINE_GENERATE, modelJson, true, null);
        } catch (Exception ex) {
            JSONObject fallbackJson = new JSONObject();
            String fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            fallbackJson.put("fallback", true);
            fallbackJson.put("fallbackReason", fallbackReason);
            logStoryLlmJson(ENDPOINT_STORY_OUTLINE_GENERATE, fallbackJson, false, fallbackReason);
            throw ex;
        }

        List<TsStoryOneClickOutlineChapterVo> chapters = StoryPromptGenerateUtil.parseOutlineChapters(modelJson.get("chapters"));
        if (chapters.isEmpty()) {
            chapters.add(StoryPromptGenerateUtil.buildFallbackOutlineChapter(modelJson));
        }

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "outline");
        snapshot.put("promptCode", templateRef.code());
        snapshot.put("promptVersion", templateRef.version());
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("chapterCount", chapters.size());
        snapshot.put("result", chapters);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "outline", user.getId(), snapshot);

        TsStoryOneClickOutlineGenerateVo vo = new TsStoryOneClickOutlineGenerateVo();
        vo.setChapters(chapters);
        vo.setPromptCode(templateRef.code());
        vo.setPromptVersion(templateRef.version());
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    private JSONObject callPromptChatWithSchemaRepair(PromptRenderedSectionsVo sections, String scene) {
        String rawContent = null;
        try {
            rawContent = promptChatService.chatToolCall(
                    sections.getDeveloperPrompt(),
                    sections.getUserPrompt(),
                    sections.getToolSchema());
            JSONObject parsed = PromptRuntimeUtil.parseJsonObject(rawContent);
            if (matchesToolSchemaRequired(parsed, sections.getToolSchema())) {
                log.info("[PROMPT_CHAT_JSON_FULL] stage=first-pass payload={}",
                        PromptRuntimeUtil.sanitizeToolCallLogJson(parsed).toJSONString());
                return parsed;
            }
            log.warn("[PROMPT_CHAT_JSON_FULL] stage=first-pass-required-mismatch scene={} payload={}",
                    scene, PromptRuntimeUtil.sanitizeToolCallLogJson(parsed).toJSONString());
        } catch (Exception firstEx) {
            log.warn("[PROMPT_CHAT_JSON_FULL] stage=first-pass-parse-fail scene={} reason={}", scene, firstEx.getMessage());
        }

        PromptTemplateRef repairTemplateRef = resolveJsonRepairTemplateRef();
        PromptRenderedSectionsVo repairPrompt = promptRenderService.renderPromptSections(jsonRepairTemplatePath(repairTemplateRef),
                buildJsonRepairVars(scene, rawContent, sections.getToolSchema()));
        String repairedContent = promptChatService.chat(repairPrompt.getRenderedPrompt());
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
        if (!matchesToolSchemaRequired(repairedJson, sections.getToolSchema())) {
            throw new JeecgBootException("AI回复字段不完整，无法匹配schema required");
        }
        log.info("[PROMPT_CHAT_JSON_FULL] stage=repair-pass payload={}",
                PromptRuntimeUtil.sanitizeToolCallLogJson(repairedJson).toJSONString());
        return repairedJson;
    }

    private Map<String, String> buildJsonRepairVars(String scene, String rawContent, String toolSchema) {
        Map<String, String> variables = new HashMap<>();
        variables.put("scene", PromptRuntimeUtil.nullableToken(scene));
        variables.put("raw_content", PromptRuntimeUtil.nullableToken(PromptRuntimeUtil.trimToNull(rawContent)));
        variables.put("tool_schema", PromptRuntimeUtil.nullableToken(PromptRuntimeUtil.trimToNull(toolSchema)));
        variables.put("required_fields", PromptRuntimeUtil.nullableToken(String.join(", ", extractRequiredFields(toolSchema))));
        return variables;
    }

    private boolean matchesToolSchemaRequired(JSONObject data, String toolSchema) {
        List<String> requiredFields = extractRequiredFields(toolSchema);
        if (requiredFields.isEmpty()) {
            return true;
        }
        if (data == null) {
            return false;
        }
        for (String field : requiredFields) {
            if (!data.containsKey(field)) {
                return false;
            }
            Object value = data.get(field);
            if (value == null) {
                return false;
            }
            if (value instanceof String str && !StringUtils.hasText(str)) {
                return false;
            }
        }
        return true;
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

    private PromptTemplateRef resolveJsonRepairTemplateRef() {
        AiragApp app = resolvePromptApp();
        if (!StringUtils.hasText(app.getMetadata())) {
            throw new JeecgBootBizTipException("当前AI应用未配置JSON修复模板信息，缺少metadata，appId=" + app.getId());
        }
        try {
            JSONObject metadata = JSONObject.parseObject(app.getMetadata());
            if (metadata == null) {
                throw new JeecgBootBizTipException("当前AI应用metadata为空对象，无法解析JSON修复模板，appId=" + app.getId());
            }

            PromptTemplateRef ref = firstNonNull(
                    parseTemplateRef(metadata.get(METADATA_TOOLCALL_REPAIR_PROMPT_KEY)),
                    parseTemplateRef(metadata.get(METADATA_JSON_REPAIR_PROMPT_KEY)),
                    parseTemplateRef(metadata.get(METADATA_STORY_REPAIR_PROMPT_KEY)));

            if (ref == null || !StringUtils.hasText(ref.code()) || !StringUtils.hasText(ref.version())) {
                throw new JeecgBootBizTipException(
                        "JSON修复模板配置不完整，请在app metadata中配置code+version（支持 "
                                + METADATA_TOOLCALL_REPAIR_PROMPT_KEY + " / "
                                + METADATA_JSON_REPAIR_PROMPT_KEY + " / "
                                + METADATA_STORY_REPAIR_PROMPT_KEY + "）");
            }
            return ref;
        } catch (JeecgBootBizTipException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("解析JSON修复模板配置失败，appId=" + app.getId()
                    + "，reason=" + ex.getMessage());
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

    private PromptTemplateRef resolvePromptTemplateRef(TemplateScene scene) {
        AiragApp app = resolvePromptApp();
        if (!StringUtils.hasText(app.getMetadata())) {
            throw new JeecgBootBizTipException("当前AI应用未配置故事模板信息，缺少metadata，appId=" + app.getId());
        }
        try {
            JSONObject metadata = JSONObject.parseObject(app.getMetadata());
            if (metadata == null) {
                throw new JeecgBootBizTipException("当前AI应用metadata为空对象，无法解析故事模板，appId=" + app.getId());
            }

            String code = null;
            String version = null;

            PromptTemplateRef globalRef = parseTemplateRef(metadata.get(METADATA_STORY_PROMPT_KEY));
            if (globalRef != null) {
                code = globalRef.code();
                version = globalRef.version();
            }

            JSONObject sceneMap = metadata.getJSONObject(METADATA_STORY_PROMPTS_KEY);
            if (sceneMap != null) {
                PromptTemplateRef sceneRef = parseTemplateRef(sceneMap.get(scene.sceneKey()));
                if (sceneRef != null) {
                    if (StringUtils.hasText(sceneRef.code())) {
                        code = sceneRef.code();
                    }
                    if (StringUtils.hasText(sceneRef.version())) {
                        version = sceneRef.version();
                    }
                }
            }

            String sceneCode = trimToNull(metadata.getString(scene.codeKey()));
            String sceneVersion = trimToNull(metadata.getString(scene.versionKey()));
            if (StringUtils.hasText(sceneCode)) {
                code = sceneCode;
            }
            if (StringUtils.hasText(sceneVersion)) {
                version = sceneVersion;
            }
            if (!StringUtils.hasText(code) || !StringUtils.hasText(version)) {
                throw new JeecgBootBizTipException("故事模板配置不完整，scene=" + scene.sceneKey()
                        + "，请在app metadata中配置code+version（支持 storyPromptTemplate 或 storyPromptTemplates." + scene.sceneKey() + "）");
            }
            return new PromptTemplateRef(code, version);
        } catch (JeecgBootBizTipException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("解析故事模板配置失败，appId=" + app.getId()
                    + "，scene=" + scene.sceneKey() + "，reason=" + ex.getMessage());
        }
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
            throw new JeecgBootBizTipException("未配置 jeecg.airag.prompt-chat.app-id，无法解析故事模板");
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

    private String jsonRepairTemplatePath(PromptTemplateRef ref) {
        return TOOLCALL_REPAIR_PROMPT_DIR + ref.code() + "_" + ref.version() + ".txt";
    }

    private enum TemplateScene {
        SETTING("setting", "storySettingPromptCode", "storySettingPromptVersion"),
        SCENE("scene", "storyScenePromptCode", "storyScenePromptVersion"),
        OUTLINE("outline", "storyOutlinePromptCode", "storyOutlinePromptVersion");

        private final String sceneKey;
        private final String codeKey;
        private final String versionKey;

        TemplateScene(String sceneKey, String codeKey, String versionKey) {
            this.sceneKey = sceneKey;
            this.codeKey = codeKey;
            this.versionKey = versionKey;
        }

        public String sceneKey() {
            return sceneKey;
        }

        public String codeKey() {
            return codeKey;
        }

        public String versionKey() {
            return versionKey;
        }
    }

    private record PromptTemplateRef(String code, String version) {
        private String templatePath() {
            return "prompts/story/" + code + "_" + version + ".txt";
        }
    }

    /**
     * 统一打印故事一键生成场景下 MiniMax 的 JSON 输出，便于排查字段格式问题。
     */
    private void logStoryLlmJson(String endpoint, JSONObject modelJson, boolean generated, String fallbackReason) {
        JSONObject logJson = new JSONObject();
        logJson.put("endpoint", endpoint);
        logJson.put("generated", generated);
        logJson.put("fallbackReason", fallbackReason);
        logJson.put("provider", promptChatService == null ? null : promptChatService.provider());
        logJson.put("modelJson", PromptRuntimeUtil.sanitizeToolCallLogJson(modelJson));
        log.info("[STORY_LLM_JSON] {}", logJson.toJSONString());
    }
}
