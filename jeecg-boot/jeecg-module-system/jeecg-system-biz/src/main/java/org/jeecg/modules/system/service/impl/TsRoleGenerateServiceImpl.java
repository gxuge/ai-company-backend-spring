package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.dto.MiniMaxTtsRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.MiniMaxTtsResponseVo;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.dto.tsroleimagegeneraterecord.TsRoleImageGenerateRecordSaveDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.service.ITsRoleImageGenerateRecordService;
import org.jeecg.modules.system.service.ITsUserImageAssetService;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.jeecg.modules.system.util.RoleGenerateSnapshotUtil;
import org.jeecg.modules.system.util.VoiceProfileMatchUtil;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.jeecg.modules.system.vo.tsroleimagegeneraterecord.TsRoleImageGenerateRecordVo;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 角色一键生成服务实现。
 */
@Service
public class TsRoleGenerateServiceImpl implements ITsRoleGenerateService {
    private static final String PROMPT_VERSION = "v1";
    private static final String PROMPT_CODE_SETTING = "role_core_fill";
    private static final String PROMPT_CODE_GENERATE_ROLE = "role_generate_role";
    private static final String PROMPT_CODE_IMAGE = "role_image_generate";
    private static final String PROMPT_CODE_VOICE = "role_voice_generate";
    private static final String PROMPT_PATH_SETTING = "prompts/role/role_core_fill_v1.txt";
    private static final String PROMPT_PATH_GENERATE_ROLE = "prompts/role/role_generate_role_v1.txt";
    private static final String PROMPT_PATH_IMAGE = "prompts/role/role_image_generate_v1.txt";
    private static final String PROMPT_PATH_VOICE = "prompts/role/role_voice_generate_v1.txt";
    private static final String REDIS_SNAPSHOT_PREFIX = "ts:role:generate:snapshot:";
    private static final long REDIS_SNAPSHOT_TTL_HOURS = 72L;
    private static final String DEFAULT_PREVIEW_TEXT = "你好呀，很高兴认识你。";

    @Resource
    private IMiniMaxDemoService miniMaxDemoService;
    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private ITsUserImageAssetService tsUserImageAssetService;
    @Resource
    private ITsRoleImageGenerateRecordService tsRoleImageGenerateRecordService;
    @Resource
    private TsVoiceProfileMapper tsVoiceProfileMapper;
    @Resource
    private TsRoleMapper tsRoleMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 一键补全角色设定。
     * 当前实现为纯模板驱动：后端只负责变量代入、模型调用、快照保存与结果回传。
     */
    @Override
    public TsRoleOneClickSettingGenerateVo generateRoleSetting(LoginUser user, TsRoleOneClickSettingGenerateDto request) {
        // 请求归一化 + 模板渲染 + LLM 调用：后端仅负责变量代入与结果获取。
        TsRoleOneClickSettingGenerateDto dto = request == null ? new TsRoleOneClickSettingGenerateDto() : request;
        dto.normalize();

        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_SETTING,
                PromptRuntimeUtil.buildSettingVars(dto.getRoleName(), dto.getGender(), dto.getOccupation(), dto.getBackgroundStory(),
                        dto.getStyleHint(), dto.getKeywords()));
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);

        // 从模型结果中读取角色设定四核心字段。
        String roleName = PromptRuntimeUtil.trimToNull(modelJson.getString("role_name"));
        String gender = PromptRuntimeUtil.normalizeGender(modelJson.getString("gender"));
        String occupation = PromptRuntimeUtil.trimToNull(modelJson.getString("occupation"));
        String backgroundStory = PromptRuntimeUtil.trimToNull(modelJson.getString("background_story"));

        // 生成并保存快照：记录渲染后的 prompt、模型原始响应与结构化结果，便于追溯。
        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "setting");
        snapshot.put("promptCode", PROMPT_CODE_SETTING);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        JSONObject resultJson = new JSONObject();
        resultJson.put("role_name", roleName);
        resultJson.put("gender", gender);
        resultJson.put("occupation", occupation);
        resultJson.put("background_story", backgroundStory);
        snapshot.put("result", resultJson);
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "setting", user.getId(), snapshot);

        // 组装响应给前端。
        TsRoleOneClickSettingGenerateVo vo = new TsRoleOneClickSettingGenerateVo();
        vo.setRoleName(roleName);
        vo.setGender(gender);
        vo.setOccupation(occupation);
        vo.setBackgroundStory(backgroundStory);
        vo.setFilledFields(null);
        vo.setKeptFields(null);
        vo.setPromptCode(PROMPT_CODE_SETTING);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 一键生成角色形象。
     * 可无条件生成；若传入角色设定字段则作为上下文参与提示词构建。
     */
    @Override
    public TsRoleOneClickImageGenerateVo generateRoleImage(LoginUser user, TsRoleOneClickImageGenerateDto request) {
        // 请求归一化；如传 roleId 则做归属校验并加载角色实体。
        TsRoleOneClickImageGenerateDto dto = request == null ? new TsRoleOneClickImageGenerateDto() : request;
        dto.normalize();

        TsRole role = null;
        if (dto.getRoleId() != null) {
            role = tsRoleMapper.selectOwned(dto.getRoleId(), user.getId());
            if (role == null) {
                throw new JeecgBootException("角色不存在或无权访问");
            }
        }

        // 构建形象 prompt，并调用 LLM 产出文生图描述。
        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_IMAGE,
                PromptRuntimeUtil.buildImageVars(dto.getRoleName(), dto.getGender(), dto.getOccupation(), dto.getBackgroundStory(),
                        dto.getStyleName(), dto.getAspectRatio(), dto.getReferenceImageUrl()));
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);
        String imagePrompt = PromptRuntimeUtil.firstNonBlank(PromptRuntimeUtil.trimToNull(modelJson.getString("visual_prompt")), renderedPrompt);

        // 调 MiniMax 文生图接口并提取首个可用图片地址。
        MiniMaxImageRequestDto imageRequest = new MiniMaxImageRequestDto();
        imageRequest.setPrompt(imagePrompt);
        MiniMaxImageResponseVo imageResponse = miniMaxDemoService.image(imageRequest);

        String imageUrl = null;
        if (imageResponse != null && imageResponse.getImageUrls() != null) {
            for (String url : imageResponse.getImageUrls()) {
                if (StringUtils.hasText(url)) {
                    imageUrl = url.trim();
                    break;
                }
            }
        }
        if (!StringUtils.hasText(imageUrl)) {
            throw new JeecgBootException("形象生成失败，未返回图片地址");
        }

        // 落库到用户图片资产表，并获取资产 ID。
        TsUserImageAssetSaveDto saveAsset = new TsUserImageAssetSaveDto();
        saveAsset.setFileUrl(imageUrl);
        saveAsset.setThumbnailUrl(imageUrl);
        saveAsset.setFileName("role-image-" + System.currentTimeMillis() + ".png");
        saveAsset.setSourceType("ai_generate");
        Result<TsUserImageAssetVo> assetResult = tsUserImageAssetService.addAsset(user, saveAsset);
        Long assetId = assetResult.getResult() == null ? null : assetResult.getResult().getId();

        // 若绑定角色：写入生图记录并回写角色头像。
        Long recordId = null;
        if (role != null) {
            TsRoleImageGenerateRecordSaveDto saveRecord = new TsRoleImageGenerateRecordSaveDto();
            saveRecord.setRoleId(role.getId());
            saveRecord.setPromptText(imagePrompt);
            saveRecord.setStyleName(dto.getStyleName());
            saveRecord.setSourceProfileUrl(dto.getReferenceImageUrl());
            saveRecord.setGenerateStatus("success");
            saveRecord.setApplyStatus("applied");
            saveRecord.setResultAssetId(assetId);
            saveRecord.setResultImageUrl(imageUrl);
            saveRecord.setExtJson(modelJson.toJSONString());
            Result<TsRoleImageGenerateRecordVo> recordResult = tsRoleImageGenerateRecordService.addRecord(user, role.getId(), saveRecord);
            if (recordResult.getResult() != null) {
                recordId = recordResult.getResult().getId();
            }
            role.setAvatarUrl(imageUrl);
            role.setUpdatedAt(new Date());
            tsRoleMapper.updateById(role);
        }

        // 生成快照并写入 Redis。
        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "image");
        snapshot.put("promptCode", PROMPT_CODE_IMAGE);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("imagePrompt", imagePrompt);
        snapshot.put("resultImageUrl", imageUrl);
        snapshot.put("assetId", assetId);
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "image", user.getId(), snapshot);

        // 组装响应给前端。
        TsRoleOneClickImageGenerateVo vo = new TsRoleOneClickImageGenerateVo();
        vo.setImageUrl(imageUrl);
        vo.setImageAssetId(assetId);
        vo.setGenerateRecordId(recordId);
        vo.setImagePrompt(imagePrompt);
        vo.setPromptCode(PROMPT_CODE_IMAGE);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 一键生成角色声音。
     * 可无条件生成；若传入角色设定字段则作为上下文参与推荐。
     */
    @Override
    public TsRoleOneClickVoiceGenerateVo generateRoleVoice(LoginUser user, TsRoleOneClickVoiceGenerateDto request) {
        // 请求归一化；如传 roleId 则做归属校验并加载角色实体。
        TsRoleOneClickVoiceGenerateDto dto = request == null ? new TsRoleOneClickVoiceGenerateDto() : request;
        dto.normalize();

        TsRole role = null;
        if (dto.getRoleId() != null) {
            role = tsRoleMapper.selectOwned(dto.getRoleId(), user.getId());
            if (role == null) {
                throw new JeecgBootException("角色不存在或无权访问");
            }
        }

        // 构建声音 prompt 并调用 LLM 获取推荐信息。
        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_VOICE,
                PromptRuntimeUtil.buildVoiceVars(dto.getRoleName(), dto.getGender(), dto.getOccupation(), dto.getBackgroundStory(),
                        dto.getPreferredVoiceName(), dto.getTargetTone(), dto.getPreviewText()));
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);
        String preferredVoiceName = PromptRuntimeUtil.firstNonBlank(dto.getPreferredVoiceName(), PromptRuntimeUtil.trimToNull(modelJson.getString("voice_name")));
        String recommendedGender = PromptRuntimeUtil.firstNonBlank(dto.getGender(), PromptRuntimeUtil.normalizeGender(modelJson.getString("gender")));
        String previewText = PromptRuntimeUtil.firstNonBlank(dto.getPreviewText(), PromptRuntimeUtil.trimToNull(modelJson.getString("preview_text")), DEFAULT_PREVIEW_TEXT);
        String recommendation = PromptRuntimeUtil.trimToNull(modelJson.getString("selection_reason"));

        // 从本地音色库匹配可用音色：先按性别主候选，再做无性别兜底。
        List<TsVoiceProfile> primaryProfiles = VoiceProfileMatchUtil.queryVoiceProfiles(tsVoiceProfileMapper, recommendedGender, 20);
        List<TsVoiceProfile> fallbackProfiles = StringUtils.hasText(recommendedGender)
                ? VoiceProfileMatchUtil.queryVoiceProfiles(tsVoiceProfileMapper, null, 20)
                : primaryProfiles;
        TsVoiceProfile selected = VoiceProfileMatchUtil.selectBestVoiceProfile(primaryProfiles, fallbackProfiles, preferredVoiceName);
        if (selected == null) {
            throw new JeecgBootException("未找到可用音色，请先维护公共音色");
        }

        // 使用 providerVoiceId 调 MiniMax TTS 生成试听音频。
        String previewAudioUrl = null;
        if (StringUtils.hasText(selected.getProviderVoiceId())) {
            MiniMaxTtsRequestDto ttsRequest = new MiniMaxTtsRequestDto();
            ttsRequest.setText(previewText);
            ttsRequest.setVoiceId(selected.getProviderVoiceId());
            MiniMaxTtsResponseVo ttsResponse = miniMaxDemoService.tts(ttsRequest);
            previewAudioUrl = ttsResponse == null ? null : PromptRuntimeUtil.trimToNull(ttsResponse.getAudioUrl());
        }

        // 若绑定角色，则回写角色音色名。
        if (role != null) {
            role.setVoiceName(selected.getName());
            role.setUpdatedAt(new Date());
            tsRoleMapper.updateById(role);
        }

        // 生成快照并写入 Redis。
        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "voice");
        snapshot.put("promptCode", PROMPT_CODE_VOICE);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("voiceProfileId", selected.getId());
        snapshot.put("voiceName", selected.getName());
        snapshot.put("previewAudioUrl", previewAudioUrl);
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "voice", user.getId(), snapshot);

        // 组装响应给前端。
        TsRoleOneClickVoiceGenerateVo vo = new TsRoleOneClickVoiceGenerateVo();
        vo.setVoiceProfileId(selected.getId());
        vo.setVoiceName(selected.getName());
        vo.setProviderVoiceId(selected.getProviderVoiceId());
        vo.setRecommendation(recommendation);
        vo.setPreviewText(previewText);
        vo.setPreviewAudioUrl(previewAudioUrl);
        vo.setPromptCode(PROMPT_CODE_VOICE);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    /**
     * 随机生成完整角色（设定+形象+声音）。
     * 输入的 storySetting/storyBackground 可为空，空值由模板与模型自动补全。
     */
    @Override
    public TsRoleGenerateRoleVo generateRole(LoginUser user, TsRoleGenerateRoleDto request) {
        // 先根据场景设定渲染模板，生成角色四核心字段以及形象/声音偏好线索。
        TsRoleGenerateRoleDto dto = request == null ? new TsRoleGenerateRoleDto() : request;
        dto.normalize();
        String renderedPrompt = promptRenderService.renderPrompt(PROMPT_PATH_GENERATE_ROLE,
                PromptRuntimeUtil.buildGenerateRoleVars(dto.getStorySetting(), dto.getStoryBackground()));
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(miniMaxDemoService, renderedPrompt);

        // 抽取模型结果，并为关键字段提供最小兜底，确保可创建完整角色。
        String roleName = PromptRuntimeUtil.firstNonBlank(PromptRuntimeUtil.trimToNull(modelJson.getString("role_name")),
                "角色" + System.currentTimeMillis());
        String gender = PromptRuntimeUtil.firstNonBlank(PromptRuntimeUtil.normalizeGender(modelJson.getString("gender")), "unknown");
        String occupation = PromptRuntimeUtil.firstNonBlank(PromptRuntimeUtil.trimToNull(modelJson.getString("occupation")), "待定职业");
        String backgroundStory = PromptRuntimeUtil.firstNonBlank(PromptRuntimeUtil.trimToNull(modelJson.getString("background_story")),
                "这是一位等待你进一步完善故事设定的角色。");
        String styleName = PromptRuntimeUtil.trimToNull(modelJson.getString("style_name"));
        String preferredVoiceName = PromptRuntimeUtil.trimToNull(modelJson.getString("preferred_voice_name"));
        String targetTone = PromptRuntimeUtil.trimToNull(modelJson.getString("target_tone"));
        String previewText = PromptRuntimeUtil.trimToNull(modelJson.getString("preview_text"));

        // 先落库角色主记录，再复用已有形象/声音一键能力继续完善资源与头像/音色。
        TsRole role = new TsRole();
        role.setUserId(user.getId());
        role.setRoleName(roleName);
        role.setGender(gender);
        role.setOccupation(occupation);
        role.setBackgroundStory(backgroundStory);
        role.setStatus(1);
        role.setIsPublic(0);
        role.setBasicAiGenerated(1);
        role.setAdvancedAiGenerated(1);
        role.setCreatedAt(new Date());
        role.setUpdatedAt(new Date());
        tsRoleMapper.insert(role);

        TsRoleOneClickImageGenerateDto imageRequest = new TsRoleOneClickImageGenerateDto();
        imageRequest.setRoleId(role.getId());
        imageRequest.setRoleName(roleName);
        imageRequest.setGender(gender);
        imageRequest.setOccupation(occupation);
        imageRequest.setBackgroundStory(backgroundStory);
        imageRequest.setStyleName(styleName);
        imageRequest.setAspectRatio(null);
        imageRequest.setReferenceImageUrl(null);
        TsRoleOneClickImageGenerateVo imageResult = generateRoleImage(user, imageRequest);

        TsRoleOneClickVoiceGenerateDto voiceRequest = new TsRoleOneClickVoiceGenerateDto();
        voiceRequest.setRoleId(role.getId());
        voiceRequest.setRoleName(roleName);
        voiceRequest.setGender(gender);
        voiceRequest.setOccupation(occupation);
        voiceRequest.setBackgroundStory(backgroundStory);
        voiceRequest.setPreferredVoiceName(preferredVoiceName);
        voiceRequest.setTargetTone(targetTone);
        voiceRequest.setPreviewText(previewText);
        TsRoleOneClickVoiceGenerateVo voiceResult = generateRoleVoice(user, voiceRequest);

        // 组装设定结果与总快照，方便前端一次拿到完整链路结果并可追溯。
        TsRoleOneClickSettingGenerateVo settingResult = new TsRoleOneClickSettingGenerateVo();
        settingResult.setRoleName(roleName);
        settingResult.setGender(gender);
        settingResult.setOccupation(occupation);
        settingResult.setBackgroundStory(backgroundStory);
        settingResult.setFilledFields(null);
        settingResult.setKeptFields(null);
        settingResult.setPromptCode(PROMPT_CODE_GENERATE_ROLE);
        settingResult.setPromptVersion(PROMPT_VERSION);
        settingResult.setRenderedPrompt(renderedPrompt);
        settingResult.setSnapshotKey(null);

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "generate-role");
        snapshot.put("promptCode", PROMPT_CODE_GENERATE_ROLE);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("storySetting", dto.getStorySetting());
        snapshot.put("storyBackground", dto.getStoryBackground());
        snapshot.put("roleId", role.getId());
        snapshot.put("settingResult", settingResult);
        snapshot.put("imageSnapshotKey", imageResult.getSnapshotKey());
        snapshot.put("voiceSnapshotKey", voiceResult.getSnapshotKey());
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "generate-role", user.getId(), snapshot);

        // 返回完整角色结果。
        TsRoleGenerateRoleVo vo = new TsRoleGenerateRoleVo();
        vo.setRoleId(role.getId());
        vo.setSettingResult(settingResult);
        vo.setImageResult(imageResult);
        vo.setVoiceResult(voiceResult);
        vo.setPromptCode(PROMPT_CODE_GENERATE_ROLE);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(renderedPrompt);
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

}
