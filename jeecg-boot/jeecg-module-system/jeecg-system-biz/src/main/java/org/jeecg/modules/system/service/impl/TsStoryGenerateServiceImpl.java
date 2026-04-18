package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.PromptRenderService;
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

import java.util.List;

/**
 * 故事生成服务实现。
 */
@Service
@Slf4j
public class TsStoryGenerateServiceImpl implements ITsStoryGenerateService {
    private static final String PROMPT_VERSION = "v1";
    private static final String PROMPT_CODE_SETTING = "story_core_fill";
    private static final String PROMPT_CODE_SCENE = "story_scene_generate";
    private static final String PROMPT_CODE_OUTLINE = "story_outline_generate";
    private static final String PROMPT_PATH_SETTING = "prompts/story/story_core_fill_v1.txt";
    private static final String PROMPT_PATH_SCENE = "prompts/story/story_scene_generate_v1.txt";
    private static final String PROMPT_PATH_OUTLINE = "prompts/story/story_outline_generate_v1.txt";
    private static final String REDIS_SNAPSHOT_PREFIX = "ts:story:generate:snapshot:";
    private static final long REDIS_SNAPSHOT_TTL_HOURS = 72L;
    private static final String ENDPOINT_STORY_SETTING_GENERATE = "/sys/ts-stories/story-setting-generate";
    private static final String ENDPOINT_STORY_SCENE_GENERATE = "/sys/ts-stories/story--scene-generate";
    private static final String ENDPOINT_STORY_OUTLINE_GENERATE = "/sys/ts-stories/story--outline-generate";

    @Resource
    private IMiniMaxDemoService miniMaxDemoService;
    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成故事设定（标题/简介/模式/设定/背景）。
     */
    @Override
    public TsStoryOneClickSettingGenerateVo generateStorySetting(LoginUser user, TsStoryOneClickSettingGenerateDto request) {
        TsStoryOneClickSettingGenerateDto dto = request == null ? new TsStoryOneClickSettingGenerateDto() : request;
        dto.normalize();

        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_SETTING, StoryPromptGenerateUtil.buildSettingVars(dto));
        JSONObject modelJson;
        boolean generated = true;
        String fallbackReason = null;
        try {
            modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);
        } catch (Exception ex) {
            modelJson = new JSONObject();
            modelJson.put("fallback", true);
            fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            modelJson.put("fallbackReason", fallbackReason);
            generated = false;
        }
        logStoryMinimaxJson(ENDPOINT_STORY_SETTING_GENERATE, modelJson, generated, fallbackReason);

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
        snapshot.put("promptCode", PROMPT_CODE_SETTING);
        snapshot.put("promptVersion", PROMPT_VERSION);
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
        vo.setPromptCode(PROMPT_CODE_SETTING);
        vo.setPromptVersion(PROMPT_VERSION);
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

        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_SCENE, StoryPromptGenerateUtil.buildSceneVars(dto));
        JSONObject modelJson;
        boolean generated = true;
        String fallbackReason = null;
        try {
            modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);
        } catch (Exception ex) {
            modelJson = new JSONObject();
            modelJson.put("fallback", true);
            fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            modelJson.put("fallbackReason", fallbackReason);
            generated = false;
        }
        logStoryMinimaxJson(ENDPOINT_STORY_SCENE_GENERATE, modelJson, generated, fallbackReason);

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
        snapshot.put("promptCode", PROMPT_CODE_SCENE);
        snapshot.put("promptVersion", PROMPT_VERSION);
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
        vo.setPromptCode(PROMPT_CODE_SCENE);
        vo.setPromptVersion(PROMPT_VERSION);
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

        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_OUTLINE, StoryPromptGenerateUtil.buildOutlineVars(dto));
        JSONObject modelJson;
        try {
            modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);
            logStoryMinimaxJson(ENDPOINT_STORY_OUTLINE_GENERATE, modelJson, true, null);
        } catch (Exception ex) {
            JSONObject fallbackJson = new JSONObject();
            String fallbackReason = PromptRuntimeUtil.trimToNull(ex.getMessage());
            fallbackJson.put("fallback", true);
            fallbackJson.put("fallbackReason", fallbackReason);
            logStoryMinimaxJson(ENDPOINT_STORY_OUTLINE_GENERATE, fallbackJson, false, fallbackReason);
            throw ex;
        }

        List<TsStoryOneClickOutlineChapterVo> chapters = StoryPromptGenerateUtil.parseOutlineChapters(modelJson.get("chapters"));
        if (chapters.isEmpty()) {
            chapters.add(StoryPromptGenerateUtil.buildFallbackOutlineChapter(modelJson));
        }

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "outline");
        snapshot.put("promptCode", PROMPT_CODE_OUTLINE);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("chapterCount", chapters.size());
        snapshot.put("result", chapters);
        String snapshotKey = StoryGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "outline", user.getId(), snapshot);

        TsStoryOneClickOutlineGenerateVo vo = new TsStoryOneClickOutlineGenerateVo();
        vo.setChapters(chapters);
        vo.setPromptCode(PROMPT_CODE_OUTLINE);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 统一打印故事一键生成场景下 MiniMax 的 JSON 输出，便于排查字段格式问题。
     */
    private void logStoryMinimaxJson(String endpoint, JSONObject modelJson, boolean generated, String fallbackReason) {
        JSONObject logJson = new JSONObject();
        logJson.put("endpoint", endpoint);
        logJson.put("generated", generated);
        logJson.put("fallbackReason", fallbackReason);
        logJson.put("modelJson", modelJson);
        log.info("[STORY_MINIMAX_JSON] {}", logJson.toJSONString());
    }
}
