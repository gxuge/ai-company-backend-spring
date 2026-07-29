package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickOutlineGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneImageGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSettingGenerateDto;
import org.jeecg.modules.system.entity.TsPreset;
import org.jeecg.modules.system.entity.TsPresetTag;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.mapper.TsPresetMapper;
import org.jeecg.modules.system.mapper.TsPresetTagMapper;
import org.jeecg.modules.system.mapper.TsTagMapper;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.jeecg.modules.system.util.StoryGenerateSnapshotUtil;
import org.jeecg.modules.system.util.StoryPromptGenerateUtil;
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGeneratePresetTagVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineChapterVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneImageGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSettingGenerateVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 故事生成服务实现。
 */
@Service
public class TsStoryGenerateServiceImpl implements ITsStoryGenerateService {
    private static final String METADATA_STORY_PROMPT_KEY = "storyPromptTemplate";
    private static final String METADATA_STORY_PROMPTS_KEY = "storyPromptTemplates";
    private static final String REDIS_SNAPSHOT_PREFIX = "ts:story:generate:snapshot:";
    private static final long REDIS_SNAPSHOT_TTL_HOURS = 72L;
    private static final String ENDPOINT_STORY_SETTING_GENERATE = "/sys/ts-stories/story-setting-generate";
    private static final String ENDPOINT_STORY_SCENE_GENERATE = "/sys/ts-stories/story--scene-generate";
    private static final String ENDPOINT_STORY_OUTLINE_GENERATE = "/sys/ts-stories/story--outline-generate";
    private static final String ENDPOINT_STORY_FULL_GENERATE = "/sys/ts-stories/story-full-generate";
    private static final String ENDPOINT_STORY_FULL_GENERATE_PRESET = "/sys/ts-stories/story-full-generate-preset";
    private static final String PROMPT_CODE_STORY_SCENE = "story_scene_generate";
    private static final String PROMPT_VERSION_STORY_SCENE = "v1";
    private static final String PROMPT_CODE_STORY_SCENE_IMAGE = "story_scene_image_generate";
    private static final String PROMPT_VERSION_STORY_SCENE_IMAGE = "v1";
    private static final String PROMPT_VERSION_V2 = "v2";
    private static final String PROMPT_CODE_STORY_FULL = "story_core_fill";
    private static final String PROMPT_CODE_STORY_SETTING_OPTIMIZE = "story_setting_optimize";
    private static final String PROMPT_CODE_STORY_SITE_SETTING_OPTIMIZE = "story_site_setting_optimize";
    private static final String PROMPT_CODE_STORY_PLOT_OUTLINE_OPTIMIZE = "story_plot_outline_optimize";
    private static final String PROMPT_CODE_STORY_FULL_PRESET = "story_core_fill_preset";
    private static final String PROMPT_VERSION_STORY_FULL_PRESET = "v2";
    private static final String TAG_TYPE_TITLE = "title";
    private static final String TAG_TYPE_NARRATIVE_STYLE = "narrative_style";
    private static final String TAG_TYPE_STORY_BACKGROUND = "story_background";
    private static final String TAG_TYPE_USER_ROLE = "user_role";
    private static final String TAG_TYPE_STORY_RULE = "story_rule";
    private static final String TAG_TYPE_BOUNDARY_RULE = "boundary_rule";
    private static final String TAG_TYPE_LOCATION = "location";
    private static final String TAG_TYPE_TIME_PERIOD = "time_period";
    private static final String TAG_TYPE_PLOT_HOOK = "plot_hook";
    private static final String TAG_TYPE_CONFLICT = "conflict";
    private static final String TAG_TYPE_PROGRESSION_MODE = "progression_mode";

    @Resource
    private IPromptChatService promptChatService;
    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private IMiniMaxDemoService miniMaxDemoService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PromptChatConfigBean promptChatConfigBean;
    @Resource
    private AiragAppMapper airagAppMapper;
    @Resource
    private TsPresetMapper tsPresetMapper;
    @Resource
    private TsPresetTagMapper tsPresetTagMapper;
    @Resource
    private TsTagMapper tsTagMapper;
    @Resource
    private ToolcallJsonRepairService toolcallJsonRepairService;

    /**
     * 生成故事设定（标题/简介/模式/设定/背景）。
     */
    @Override
    public TsStoryOneClickSettingGenerateVo generateStorySetting(LoginUser user, TsStoryOneClickSettingGenerateDto request) {
        TsStoryOneClickSettingGenerateDto dto = request == null ? new TsStoryOneClickSettingGenerateDto() : request;
        dto.normalize();
        boolean settingOptimizeMode = dto.isSettingOptimizeMode();
        PromptTemplateRef templateRef = settingOptimizeMode
                ? new PromptTemplateRef(PROMPT_CODE_STORY_SETTING_OPTIMIZE, PROMPT_VERSION_V2)
                : resolvePromptTemplateRef(TemplateScene.SETTING);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                templateRef.code(), templateRef.version(),
                settingOptimizeMode ? StoryPromptGenerateUtil.buildSettingOptimizeVars(dto) : StoryPromptGenerateUtil.buildSettingVars(dto));
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
        String storySetting;
        if (settingOptimizeMode) {
            storySetting = PromptRuntimeUtil.firstNonBlank(
                    PromptRuntimeUtil.trimToNull(modelJson.getString("story_setting")),
                    dto.getStorySetting());
        } else {
            storySetting = PromptRuntimeUtil.firstNonBlank(
                    PromptRuntimeUtil.trimToNull(modelJson.getString("story_setting")),
                    dto.getStorySetting());
        }
        String title = settingOptimizeMode ? null : PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("title")),
                dto.getTitle(),
                "Original Story " + System.currentTimeMillis());
        String storyIntro = settingOptimizeMode ? null : PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_intro")),
                dto.getStoryIntro());
        String storyMode = settingOptimizeMode ? null : StoryPromptGenerateUtil.normalizeStoryMode(PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_mode")),
                dto.getStoryMode(),
                "chapter"));
        String storyBackground = settingOptimizeMode ? null : PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_background")),
                dto.getStoryBackground());

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "story-setting");
        snapshot.put("promptCode", templateRef.code());
        snapshot.put("promptVersion", templateRef.version());
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        JSONObject resultJson = new JSONObject();
        resultJson.put("story_setting", storySetting);
        if (!settingOptimizeMode) {
            resultJson.put("title", title);
            resultJson.put("story_intro", storyIntro);
            resultJson.put("story_mode", storyMode);
            resultJson.put("story_background", storyBackground);
        }
        snapshot.put("result", resultJson);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "setting", user.getId(), snapshot);

        TsStoryOneClickSettingGenerateVo vo = new TsStoryOneClickSettingGenerateVo();
        vo.setStorySetting(storySetting);
        if (!settingOptimizeMode) {
            vo.setTitle(title);
            vo.setStoryIntro(storyIntro);
            vo.setStoryMode(storyMode);
            vo.setStoryBackground(storyBackground);
        }
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
        boolean siteSettingOptimizeMode = dto.isSiteSettingOptimizeMode();
        PromptTemplateRef templateRef = siteSettingOptimizeMode
                ? new PromptTemplateRef(PROMPT_CODE_STORY_SITE_SETTING_OPTIMIZE, PROMPT_VERSION_V2)
                : new PromptTemplateRef(PROMPT_CODE_STORY_SCENE, PROMPT_VERSION_STORY_SCENE);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                templateRef.code(), templateRef.version(),
                siteSettingOptimizeMode ? StoryPromptGenerateUtil.buildSceneOptimizeVars(dto) : StoryPromptGenerateUtil.buildSceneVars(dto));
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
        String sceneSummary;
        if (siteSettingOptimizeMode) {
            sceneSummary = PromptRuntimeUtil.firstNonBlank(
                    PromptRuntimeUtil.trimToNull(modelJson.getString("site_setting")),
                    PromptRuntimeUtil.trimToNull(modelJson.getString("scene_summary")),
                    PromptRuntimeUtil.trimToNull(modelJson.getString("scene_desc")),
                    dto.getSceneSetting(),
                    "这是一个等待你继续完善的场景。");
        } else {
            String sceneNameSnapshot = PromptRuntimeUtil.firstNonBlank(
                    PromptRuntimeUtil.trimToNull(modelJson.getString("scene_name_snapshot")),
                    PromptRuntimeUtil.trimToNull(modelJson.getString("scene_name")),
                    dto.getSceneSetting(),
                    "未命名场景");
            sceneSummary = PromptRuntimeUtil.firstNonBlank(
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
        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "scene-setting");
        snapshot.put("promptCode", templateRef.code());
        snapshot.put("promptVersion", templateRef.version());
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        JSONObject resultJson = new JSONObject();
        resultJson.put("scene_summary", sceneSummary);
        snapshot.put("result", resultJson);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "scene", user.getId(), snapshot);

        TsStoryOneClickSceneGenerateVo vo = new TsStoryOneClickSceneGenerateVo();
        vo.setSceneNameSnapshot(null);
        vo.setSceneSummary(sceneSummary);
        vo.setSceneElements(null);
        vo.setGenerated(generated);
        vo.setFallbackReason(fallbackReason);
        vo.setPromptCode(templateRef.code());
        vo.setPromptVersion(templateRef.version());
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 生成临时故事场景背景图片，仅返回供应商原始图片地址。
     */
    @Override
    public TsStoryOneClickSceneImageGenerateVo generateStorySceneImage(
            LoginUser user, TsStoryOneClickSceneImageGenerateDto request) {
        TsStoryOneClickSceneImageGenerateDto dto =
                request == null ? new TsStoryOneClickSceneImageGenerateDto() : request;
        dto.normalize();
        if (!dto.hasSceneContext()) {
            throw new JeecgBootBizTipException("storySetting和siteSetting不能同时为空");
        }

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                PROMPT_CODE_STORY_SCENE_IMAGE,
                PROMPT_VERSION_STORY_SCENE_IMAGE,
                StoryPromptGenerateUtil.buildSceneImageVars(dto));
        JSONObject modelJson = callPromptChatWithSchemaRepair(promptSections, "scene-image");
        String visualPrompt = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("visual_prompt")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("visualPrompt"))
        );
        if (!StringUtils.hasText(visualPrompt)) {
            throw new JeecgBootBizTipException("模型未返回visual_prompt，无法生成故事场景背景图片");
        }

        String styleName = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("style_name")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("styleName")),
                dto.getStyleName()
        );
        String aspectRatio = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("aspect_ratio")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("aspectRatio")),
                dto.getAspectRatio()
        );

        MiniMaxImageRequestDto imageRequest = new MiniMaxImageRequestDto();
        imageRequest.setPrompt(composeSceneImagePrompt(visualPrompt, styleName, aspectRatio));
        imageRequest.setReferenceImageUrl(dto.getReferenceImageUrl());
        imageRequest.setUploadGeneratedMedia(Boolean.FALSE);
        MiniMaxImageResponseVo imageResponse = miniMaxDemoService.image(imageRequest);
        String imageUrl = firstOriginalImageUrl(imageResponse);
        if (!StringUtils.hasText(imageUrl)) {
            throw new JeecgBootBizTipException("故事场景背景图片生成失败，未返回图片地址");
        }

        TsStoryOneClickSceneImageGenerateVo vo = new TsStoryOneClickSceneImageGenerateVo();
        vo.setImageUrl(imageUrl);
        vo.setPromptCode(PROMPT_CODE_STORY_SCENE_IMAGE);
        vo.setPromptVersion(PROMPT_VERSION_STORY_SCENE_IMAGE);
        return vo;
    }

    /**
     * 拼装传给图片模型的场景提示词。
     */
    private String composeSceneImagePrompt(String visualPrompt, String styleName, String aspectRatio) {
        StringBuilder builder = new StringBuilder("visual_prompt: ").append(visualPrompt.trim());
        if (StringUtils.hasText(styleName)) {
            builder.append('\n').append("style_name: ").append(styleName.trim());
        }
        if (StringUtils.hasText(aspectRatio)) {
            builder.append('\n').append("aspect_ratio: ").append(aspectRatio.trim());
        }
        return builder.toString();
    }

    /**
     * 从生图响应中读取首个供应商原始图片地址。
     */
    private String firstOriginalImageUrl(MiniMaxImageResponseVo imageResponse) {
        if (imageResponse == null || imageResponse.getOriginalImageUrls() == null) {
            return null;
        }
        for (String url : imageResponse.getOriginalImageUrls()) {
            if (StringUtils.hasText(url)) {
                return url.trim();
            }
        }
        return null;
    }

    /**
     * 生成剧情大纲（章节数组）。
     */
    @Override
    public TsStoryOneClickOutlineGenerateVo generateStoryOutline(LoginUser user, TsStoryOneClickOutlineGenerateDto request) {
        TsStoryOneClickOutlineGenerateDto dto = request == null ? new TsStoryOneClickOutlineGenerateDto() : request;
        dto.normalize();
        boolean chapterMode = "chapter".equals(dto.getStoryMode());
        PromptTemplateRef templateRef = chapterMode
                ? resolvePromptTemplateRef(TemplateScene.OUTLINE)
                : new PromptTemplateRef(PROMPT_CODE_STORY_PLOT_OUTLINE_OPTIMIZE, PROMPT_VERSION_V2);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                templateRef.code(), templateRef.version(),
                chapterMode ? StoryPromptGenerateUtil.buildOutlineVars(dto) : StoryPromptGenerateUtil.buildOutlineOptimizeVars(dto));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson;
        try {
            modelJson = callPromptChatWithSchemaRepair(promptSections, "outline");
        } catch (Exception ex) {
            JSONObject fallbackJson = new JSONObject();
            String fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            fallbackJson.put("fallback", true);
            fallbackJson.put("fallbackReason", fallbackReason);
            throw ex;
        }

        List<TsStoryOneClickOutlineChapterVo> chapters = null;
        String plotOutline = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("plot_outline")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("outline")),
                dto.getPlotOutline()
        );
        if (chapterMode) {
            chapters = StoryPromptGenerateUtil.parseOutlineChapters(modelJson.get("chapters"));
            if (chapters.isEmpty()) {
                chapters.add(StoryPromptGenerateUtil.buildFallbackOutlineChapter(modelJson));
            }
        }

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "outline");
        snapshot.put("promptCode", templateRef.code());
        snapshot.put("promptVersion", templateRef.version());
        snapshot.put("storyMode", dto.getStoryMode());
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("plotOutline", plotOutline);
        snapshot.put("result", chapterMode ? chapters : plotOutline);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "outline", user.getId(), snapshot);

        TsStoryOneClickOutlineGenerateVo vo = new TsStoryOneClickOutlineGenerateVo();
        vo.setChapters(chapters);
        vo.setPlotOutline(plotOutline);
        vo.setGenerated(Boolean.TRUE);
        vo.setFallbackReason(null);
        vo.setPromptCode(templateRef.code());
        vo.setPromptVersion(templateRef.version());
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 通用全量生成：输入输出均为 5 个故事核心字段。
     */
    @Override
    public TsStoryFullGenerateVo generateStoryFull(LoginUser user, TsStoryFullGenerateDto request) {
        TsStoryFullGenerateDto dto = request == null ? new TsStoryFullGenerateDto() : request;
        dto.normalize();

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                PROMPT_CODE_STORY_FULL, PROMPT_VERSION_V2, buildStoryFullVars(dto));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson = callPromptChatWithSchemaRepair(promptSections, "story-full-generate");

        String title = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("title")),
                dto.getTitle());
        String storyMode = StoryPromptGenerateUtil.normalizeStoryMode(PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_mode")),
                dto.getStoryMode(),
                "chapter"));
        String storyIntro = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_intro")),
                dto.getStoryIntro());
        String storySettingText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_setting")),
                dto.getStorySetting());
        String siteSettingText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("site_setting")),
                dto.getSiteSetting());
        String plotOutlineText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("plot_outline")),
                dto.getPlotOutline());

        saveStoryFullSnapshot(user, renderedPrompt, modelJson, title, storyMode, storyIntro, storySettingText, siteSettingText, plotOutlineText);

        TsStoryFullGenerateVo vo = new TsStoryFullGenerateVo();
        vo.setTitle(title);
        vo.setStoryMode(storyMode);
        vo.setStoryIntro(storyIntro);
        vo.setStorySetting(storySettingText);
        vo.setSiteSetting(siteSettingText);
        vo.setPlotOutline(plotOutlineText);
        return vo;
    }

    /**
     * 新接口：随机 story 预设 + 读取绑定标签 + 依据固定映射构建5字段输入 -> 调用模板 -> 串联设定/场景/大纲。
     */
    @Override
    public TsStoryFullGenerateVo generateStoryFullPreset(LoginUser user, TsStoryFullGenerateDto request) {
        TsStoryFullGenerateDto dto = request == null ? new TsStoryFullGenerateDto() : request;
        dto.normalize();

        TsPreset preset = pickRandomStoryPreset();
        List<TsStoryFullGeneratePresetTagVo> presetTags = loadPresetTags(preset.getId());
        Map<String, String> tagsByType = mergeTagsByType(presetTags);

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(
                PROMPT_CODE_STORY_FULL_PRESET, PROMPT_VERSION_STORY_FULL_PRESET,
                buildStoryFullPresetVars(dto, preset, tagsByType)
        );
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson = callPromptChatWithSchemaRepair(promptSections, "story-full-generate-preset");

        String title = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("title")),
                dto.getTitle());
        String storyMode = StoryPromptGenerateUtil.normalizeStoryMode(PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_mode")),
                dto.getStoryMode(),
                "chapter"));
        String storyIntro = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_intro")),
                dto.getStoryIntro());
        String storySettingText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("story_setting")),
                dto.getStorySetting());
        String siteSettingText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("site_setting")),
                dto.getSiteSetting());
        String plotOutlineText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("plot_outline")),
                dto.getPlotOutline());

        String snapshotKey = saveStoryFullPresetSnapshot(user, preset, presetTags, tagsByType, renderedPrompt, modelJson,
                title, storyMode, storyIntro, storySettingText, siteSettingText, plotOutlineText);

        TsStoryFullGenerateVo vo = new TsStoryFullGenerateVo();
        vo.setTitle(title);
        vo.setStoryMode(storyMode);
        vo.setStoryIntro(storyIntro);
        vo.setStorySetting(storySettingText);
        vo.setSiteSetting(siteSettingText);
        vo.setPlotOutline(plotOutlineText);
        return vo;
    }

    private String saveStoryFullPresetSnapshot(LoginUser user,
                                               TsPreset preset,
                                               List<TsStoryFullGeneratePresetTagVo> presetTags,
                                               Map<String, String> tagsByType,
                                               String renderedPrompt,
                                               JSONObject modelJson,
                                               String title,
                                               String storyMode,
                                               String storyIntro,
                                               String storySetting,
                                               String siteSetting,
                                               String plotOutline) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "story-full-generate-preset");
        snapshot.put("presetId", preset == null ? null : preset.getId());
        snapshot.put("presetName", preset == null ? null : preset.getName());
        snapshot.put("presetDescription", preset == null ? null : preset.getDescription());
        snapshot.put("tagsByType", tagsByType);
        snapshot.put("presetTags", presetTags);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("title", title);
        snapshot.put("storyMode", storyMode);
        snapshot.put("storyIntro", storyIntro);
        snapshot.put("storySetting", storySetting);
        snapshot.put("siteSetting", siteSetting);
        snapshot.put("plotOutline", plotOutline);
        return StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "full-preset", user.getId(), snapshot);
    }

    private String saveStoryFullSnapshot(LoginUser user,
                                          String renderedPrompt,
                                          JSONObject modelJson,
                                          String title,
                                          String storyMode,
                                          String storyIntro,
                                          String storySetting,
                                          String siteSetting,
                                          String plotOutline) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "story-full-generate");
        snapshot.put("promptCode", PROMPT_CODE_STORY_FULL);
        snapshot.put("promptVersion", PROMPT_VERSION_V2);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("title", title);
        snapshot.put("storyMode", storyMode);
        snapshot.put("storyIntro", storyIntro);
        snapshot.put("storySetting", storySetting);
        snapshot.put("siteSetting", siteSetting);
        snapshot.put("plotOutline", plotOutline);
        return StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "full", user.getId(), snapshot);
    }

    private TsPreset pickRandomStoryPreset() {
        QueryWrapper<TsPreset> wrapper = new QueryWrapper<>();
        wrapper.eq("target_type", "story")
                .eq("enabled", 1)
                .orderByAsc("sort_order")
                .orderByAsc("id");
        List<TsPreset> presets = tsPresetMapper.selectList(wrapper);
        if (presets == null || presets.isEmpty()) {
            throw new JeecgBootBizTipException("未找到可用的 story 预设，请先初始化 ts_preset 数据");
        }
        int index = ThreadLocalRandom.current().nextInt(presets.size());
        return presets.get(index);
    }

    private List<TsStoryFullGeneratePresetTagVo> loadPresetTags(String presetId) {
        if (!StringUtils.hasText(presetId)) {
            return new ArrayList<>();
        }
        QueryWrapper<TsPresetTag> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("preset_id", presetId)
                .orderByAsc("sort_order")
                .orderByAsc("id");
        List<TsPresetTag> relations = tsPresetTagMapper.selectList(relationWrapper);
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tagIds = relations.stream()
                .map(TsPresetTag::getTagId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<TsTag> tags = tsTagMapper.selectBatchIds(tagIds);
        Map<String, TsTag> tagMap = new HashMap<>();
        if (tags != null) {
            for (TsTag tag : tags) {
                if (tag != null && StringUtils.hasText(tag.getId())) {
                    tagMap.put(tag.getId(), tag);
                }
            }
        }
        List<TsStoryFullGeneratePresetTagVo> result = new ArrayList<>();
        for (TsPresetTag relation : relations) {
            if (relation == null || !StringUtils.hasText(relation.getTagId())) {
                continue;
            }
            TsTag tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            TsStoryFullGeneratePresetTagVo vo = new TsStoryFullGeneratePresetTagVo();
            vo.setPresetTagId(relation.getId());
            vo.setTagId(tag.getId());
            vo.setTypeId(tag.getTypeId());
            vo.setTagName(tag.getName());
            vo.setPromptText(tag.getPromptText());
            vo.setRequired(relation.getRequired());
            vo.setWeightOverride(relation.getWeightOverride());
            vo.setSortOrder(relation.getSortOrder());
            result.add(vo);
        }
        return result;
    }

    private Map<String, String> mergeTagsByType(List<TsStoryFullGeneratePresetTagVo> presetTags) {
        Map<String, LinkedHashSet<String>> grouped = new HashMap<>();
        if (presetTags != null) {
            for (TsStoryFullGeneratePresetTagVo item : presetTags) {
                if (item == null) {
                    continue;
                }
                String typeId = PromptRuntimeUtil.trimToNull(item.getTypeId());
                String tagName = PromptRuntimeUtil.trimToNull(item.getTagName());
                if (!StringUtils.hasText(typeId) || !StringUtils.hasText(tagName)) {
                    continue;
                }
                grouped.computeIfAbsent(typeId, key -> new LinkedHashSet<>()).add(tagName);
            }
        }
        Map<String, String> merged = new HashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : grouped.entrySet()) {
            merged.put(entry.getKey(), String.join(", ", entry.getValue()));
        }
        return merged;
    }

    private Map<String, String> buildStoryFullPresetVars(TsStoryFullGenerateDto dto,
                                                         TsPreset preset,
                                                         Map<String, String> tagsByType) {
        Map<String, String> vars = new HashMap<>();
        vars.put("preset_name", PromptRuntimeUtil.nullableToken(preset == null ? null : preset.getName()));
        vars.put("preset_description", PromptRuntimeUtil.nullableToken(preset == null ? null : preset.getDescription()));
        vars.put("title", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_TITLE)));
        vars.put("narrative_style", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_NARRATIVE_STYLE)));
        vars.put("story_background", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_STORY_BACKGROUND)));
        vars.put("user_role", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_USER_ROLE)));
        vars.put("story_rule", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_STORY_RULE)));
        vars.put("boundary_rule", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_BOUNDARY_RULE)));
        vars.put("location", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_LOCATION)));
        vars.put("time_period", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_TIME_PERIOD)));
        vars.put("plot_hook", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_PLOT_HOOK)));
        vars.put("conflict", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_CONFLICT)));
        vars.put("progression_mode", PromptRuntimeUtil.nullableToken(tagsByType.get(TAG_TYPE_PROGRESSION_MODE)));
        return vars;
    }

    private Map<String, String> buildStoryFullVars(TsStoryFullGenerateDto dto) {
        Map<String, String> vars = new HashMap<>();
        vars.put("title", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getTitle()));
        vars.put("story_mode", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getStoryMode()));
        vars.put("story_intro", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getStoryIntro()));
        vars.put("story_setting", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getStorySetting()));
        vars.put("site_setting", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getSiteSetting()));
        vars.put("plot_outline", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getPlotOutline()));
        vars.put("extra_info", PromptRuntimeUtil.nullableToken(dto == null ? null : dto.getExtraInfo()));
        return vars;
    }

    private JSONObject callPromptChatWithSchemaRepair(PromptRenderedSectionsVo sections, String scene) {
        return toolcallJsonRepairService.chatToolCallWithSchemaRepair(sections, scene);
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
    }

}
