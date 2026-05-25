package org.jeecg.modules.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.openapi.dto.MiniMaxImageRequestDto;
import org.jeecg.modules.openapi.service.IMiniMaxDemoService;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.jeecg.modules.openapi.service.PromptRenderService;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.openapi.vo.PromptRenderedSectionsVo;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateTextByTemplateDto;
import org.jeecg.modules.system.dto.tsrole.ImageGenerateRuntimeResult;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetSaveDto;
import org.jeecg.modules.system.entity.TsRoleImageGenerateRecord;
import org.jeecg.modules.system.entity.TsRole;
import org.jeecg.modules.system.entity.TsUserVoiceConfig;
import org.jeecg.modules.system.entity.TsVoiceProfile;
import org.jeecg.modules.system.entity.TsVoiceProfileTag;
import org.jeecg.modules.system.entity.TsVoiceTag;
import org.jeecg.modules.system.mapper.TsRoleImageGenerateRecordMapper;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.mapper.TsUserVoiceConfigMapper;
import org.jeecg.modules.system.mapper.TsUserVoiceProfileMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileMapper;
import org.jeecg.modules.system.mapper.TsVoiceProfileTagMapper;
import org.jeecg.modules.system.mapper.TsVoiceTagMapper;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.service.ITsUserImageAssetService;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.jeecg.modules.system.util.RoleGenerateSnapshotUtil;
import org.jeecg.modules.system.util.VoiceProfileMatchUtil;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateTextByTemplateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVo;
import org.jeecg.modules.system.vo.tsvoiceprofile.TsVoiceProfileVoConverter;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVo;
import org.jeecg.modules.system.vo.tsvoicetag.TsVoiceTagVoConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 角色一键生成服务实现。
 */
@Slf4j
@Service
public class TsRoleGenerateServiceImpl implements ITsRoleGenerateService {
    private static final String PROMPT_VERSION = "v2";
    private static final String PROMPT_CODE_SETTING = "role_core_fill";
    private static final String PROMPT_CODE_GENERATE_ROLE = "role_generate_role";
    private static final String PROMPT_CODE_IMAGE = "role_image_generate";
    private static final String PROMPT_CODE_VOICE = "role_voice_generate";
    private static final String PROMPT_CODE_TEXT_TEMPLATE = "role_ai_generate_text";
    private static final String PROMPT_PATH_SETTING = "prompts/role/role_core_fill_v2.txt";
    private static final String PROMPT_PATH_GENERATE_ROLE = "prompts/role/role_generate_role_v2.txt";
    private static final String PROMPT_PATH_IMAGE = "prompts/role/role_image_generate_v2.txt";
    private static final String PROMPT_PATH_TEXT_TEMPLATE = "prompts/role/role_ai_generate_text_v2.txt";
    private static final String REDIS_SNAPSHOT_PREFIX = "ts:role:generate:snapshot:";
    private static final long REDIS_SNAPSHOT_TTL_HOURS = 72L;
    private static final String DEFAULT_PREVIEW_TEXT = "你好呀，很高兴认识你。";
    private static final String IMAGE_GENERATE_STATUS_PENDING = "pending";
    private static final String IMAGE_GENERATE_STATUS_RUNNING = "running";
    private static final String IMAGE_GENERATE_STATUS_SUCCESS = "success";
    private static final String IMAGE_GENERATE_STATUS_FAILED = "failed";
    private static final int IMAGE_FAIL_REASON_MAX_LENGTH = 500;
    private static final int VOICE_CANDIDATE_LIMIT = 20;
    private static final BigDecimal DEFAULT_VOICE_PITCH_PERCENT = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_VOICE_SPEED_RATE = new BigDecimal("1.00");
    private static final String PLACEHOLDER_OCCUPATION = "\u5f85\u5b9a\u804c\u4e1a";
    private static final String PLACEHOLDER_BACKGROUND_STORY = "\u8fd9\u662f\u4e00\u4e2a\u7b49\u5f85\u4f60\u7ee7\u7eed\u5b8c\u5584\u80cc\u666f\u8bbe\u5b9a\u7684\u89d2\u8272\u3002";
    private static final Pattern PLACEHOLDER_ROLE_NAME_PATTERN = Pattern.compile("^\u89d2\u8272\\d{10,}$");

    @Resource
    private IMiniMaxDemoService miniMaxDemoService;
    @Resource
    private IPromptChatService promptChatService;
    @Resource
    private PromptRenderService promptRenderService;
    @Resource
    private ITsUserImageAssetService tsUserImageAssetService;
    @Resource
    private TsVoiceProfileMapper tsVoiceProfileMapper;
    @Resource
    private TsVoiceProfileTagMapper tsVoiceProfileTagMapper;
    @Resource
    private TsVoiceTagMapper tsVoiceTagMapper;
    @Resource
    private TsUserVoiceProfileMapper tsUserVoiceProfileMapper;
    @Resource
    private TsUserVoiceConfigMapper tsUserVoiceConfigMapper;
    @Resource
    private TsRoleMapper tsRoleMapper;
    @Resource
    private TsRoleImageGenerateRecordMapper tsRoleImageGenerateRecordMapper;
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

        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(PROMPT_PATH_SETTING,
                PromptRuntimeUtil.buildSettingVars(dto.getRoleName(), dto.getGender(), dto.getOccupation(), dto.getBackgroundStory(),
                        dto.getStyleHint(), dto.getKeywords()));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson;
        try {
            modelJson = PromptRuntimeUtil.callPromptChat(promptChatService, promptSections);
        } catch (Exception ex) {
            log.warn("Role setting JSON parse failed, fallback to request/default values. roleId={}, reason={}", dto.getRoleId(), ex.getMessage());
            modelJson = new JSONObject();
            modelJson.put("fallback", true);
            modelJson.put("fallbackReason", PromptRuntimeUtil.trimToNull(ex.getMessage()));
        }

        // 从模型结果中读取角色设定四核心字段。
        String roleName = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("role_name")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("roleName")),
                dto.getRoleName()
        );
        String gender = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.normalizeGender(modelJson.getString("gender")),
                dto.getGender()
        );
        String occupation = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("occupation")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("job")),
                dto.getOccupation()
        );
        String backgroundStory = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("background_story")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("backgroundStory")),
                dto.getBackgroundStory()
        );

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
        TsRoleOneClickImageGenerateDto dto = request == null ? new TsRoleOneClickImageGenerateDto() : request;
        dto.normalize();

        TsRole role = null;
        if (dto.getRoleId() != null) {
            role = tsRoleMapper.selectOwned(dto.getRoleId(), user.getId());
            if (role == null) {
                throw new JeecgBootException("角色不存在或无权访问");
            }
        }

        // 仅在绑定角色时支持异步任务受理，避免出现无记录可轮询的场景。
        boolean asyncGenerate = Boolean.TRUE.equals(dto.getAsyncGenerate()) && role != null;
        if (asyncGenerate) {
            return submitAsyncImageGenerateTask(user, dto, role);
        }
        return generateRoleImageSynchronously(user, dto, role);
    }

    private TsRoleOneClickImageGenerateVo submitAsyncImageGenerateTask(LoginUser user, TsRoleOneClickImageGenerateDto dto, TsRole role) {
        TsRoleImageGenerateRecord record = createPendingImageGenerateRecord(user.getId(), role.getId(), dto);
        Long recordId = record.getId();
        CompletableFuture.runAsync(() -> processAsyncImageGenerateTask(user, dto, role.getId(), recordId))
                .exceptionally(ex -> {
                    log.error("提交异步角色生图任务失败 roleId={}, recordId={}", role.getId(), recordId, ex);
                    return null;
                });

        TsRoleOneClickImageGenerateVo vo = new TsRoleOneClickImageGenerateVo();
        vo.setAccepted(Boolean.TRUE);
        vo.setGenerateStatus(IMAGE_GENERATE_STATUS_PENDING);
        vo.setGenerateRecordId(recordId);
        vo.setPromptCode(PROMPT_CODE_IMAGE);
        vo.setPromptVersion(PROMPT_VERSION);
        return vo;
    }

    private TsRoleOneClickImageGenerateVo generateRoleImageSynchronously(LoginUser user, TsRoleOneClickImageGenerateDto dto, TsRole role) {
        ImageGenerateRuntimeResult runtime = executeImageGenerate(user, dto);
        Long recordId = null;
        if (role != null) {
            recordId = createSuccessImageGenerateRecord(user.getId(), role.getId(), dto, runtime);
            updateRoleAvatar(role.getId(), runtime.getImageUrl());
        }

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "image");
        snapshot.put("promptCode", PROMPT_CODE_IMAGE);
        snapshot.put("promptVersion", PROMPT_VERSION);
        snapshot.put("promptRendered", runtime.getRenderedPrompt());
        snapshot.put("imagePrompt", runtime.getImagePrompt());
        snapshot.put("resultImageUrl", runtime.getImageUrl());
        snapshot.put("assetId", runtime.getAssetId());
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "image", user.getId(), snapshot);

        TsRoleOneClickImageGenerateVo vo = new TsRoleOneClickImageGenerateVo();
        vo.setAccepted(Boolean.TRUE);
        vo.setGenerateStatus(IMAGE_GENERATE_STATUS_SUCCESS);
        vo.setImageUrl(runtime.getImageUrl());
        vo.setImageAssetId(runtime.getAssetId());
        vo.setGenerateRecordId(recordId);
        vo.setImagePrompt(runtime.getImagePrompt());
        vo.setPromptCode(PROMPT_CODE_IMAGE);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(runtime.getRenderedPrompt());
        vo.setSnapshotKey(snapshotKey);
        return vo;
    }

    private ImageGenerateRuntimeResult executeImageGenerate(LoginUser user, TsRoleOneClickImageGenerateDto dto) {
        String promptRoleName = normalizeRoleNameForPrompt(dto.getRoleName());
        String promptOccupation = normalizeRoleFieldForPrompt(dto.getOccupation());
        String promptBackgroundStory = normalizeRoleFieldForPrompt(dto.getBackgroundStory());
        PromptRenderedSectionsVo promptSections = promptRenderService.renderPromptSections(PROMPT_PATH_IMAGE,
                PromptRuntimeUtil.buildImageVars(promptRoleName, dto.getGender(), promptOccupation, promptBackgroundStory,
                        dto.getStyleName(), dto.getAspectRatio(), dto.getReferenceImageUrl()));
        String renderedPrompt = promptSections.getRenderedPrompt();
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(promptChatService, promptSections);
        String visualPrompt = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("visual_prompt")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("visualPrompt"))
        );
        if (!StringUtils.hasText(visualPrompt)) {
            throw new JeecgBootBizTipException("模型未返回 visual_prompt，已取消回退渲染模板作为生图提示词");
        }
        String imagePrompt = composeImagePrompt(visualPrompt, modelJson, dto);

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

        TsUserImageAssetSaveDto saveAsset = new TsUserImageAssetSaveDto();
        saveAsset.setFileUrl(imageUrl);
        saveAsset.setThumbnailUrl(imageUrl);
        saveAsset.setFileName("role-image-" + System.currentTimeMillis() + ".png");
        saveAsset.setSourceType("ai_generate");
        Result<TsUserImageAssetVo> assetResult = tsUserImageAssetService.addAsset(user, saveAsset);
        Long assetId = assetResult.getResult() == null ? null : assetResult.getResult().getId();

        ImageGenerateRuntimeResult runtime = new ImageGenerateRuntimeResult();
        runtime.setRenderedPrompt(renderedPrompt);
        runtime.setModelJson(modelJson);
        runtime.setImagePrompt(imagePrompt);
        runtime.setImageUrl(imageUrl);
        runtime.setAssetId(assetId);
        return runtime;
    }

    private static String normalizeRoleNameForPrompt(String roleName) {
        String normalized = PromptRuntimeUtil.trimToNull(roleName);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (PLACEHOLDER_ROLE_NAME_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private static String normalizeRoleFieldForPrompt(String value) {
        String normalized = PromptRuntimeUtil.trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (PLACEHOLDER_OCCUPATION.equals(normalized) || PLACEHOLDER_BACKGROUND_STORY.equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private static String composeImagePrompt(String visualPrompt, JSONObject modelJson, TsRoleOneClickImageGenerateDto dto) {
        String styleName = PromptRuntimeUtil.firstNonBlank(
                modelJson == null ? null : PromptRuntimeUtil.trimToNull(modelJson.getString("style_name")),
                modelJson == null ? null : PromptRuntimeUtil.trimToNull(modelJson.getString("styleName")),
                PromptRuntimeUtil.trimToNull(dto == null ? null : dto.getStyleName())
        );
        String aspectRatio = PromptRuntimeUtil.firstNonBlank(
                modelJson == null ? null : PromptRuntimeUtil.trimToNull(modelJson.getString("aspect_ratio")),
                modelJson == null ? null : PromptRuntimeUtil.trimToNull(modelJson.getString("aspectRatio")),
                PromptRuntimeUtil.trimToNull(dto == null ? null : dto.getAspectRatio())
        );

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(visualPrompt)) {
            builder.append("visual_prompt: ").append(visualPrompt.trim());
        }
        if (StringUtils.hasText(styleName)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("style_name: ").append(styleName.trim());
        }
        if (StringUtils.hasText(aspectRatio)) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("aspect_ratio: ").append(aspectRatio.trim());
        }
        return builder.length() == 0 ? visualPrompt : builder.toString();
    }

    private void processAsyncImageGenerateTask(LoginUser user, TsRoleOneClickImageGenerateDto dto, Long roleId, Long recordId) {
        markImageGenerateRunning(recordId);
        try {
            ImageGenerateRuntimeResult runtime = executeImageGenerate(user, dto);
            markImageGenerateSuccess(recordId, runtime);
            updateRoleAvatar(roleId, runtime.getImageUrl());
        }
        catch (Exception ex) {
            markImageGenerateFailed(recordId, ex);
            log.error("异步角色生图失败 roleId={}, recordId={}", roleId, recordId, ex);
        }
    }

    private TsRoleImageGenerateRecord createPendingImageGenerateRecord(String userId, Long roleId, TsRoleOneClickImageGenerateDto dto) {
        Date now = new Date();
        TsRoleImageGenerateRecord record = new TsRoleImageGenerateRecord();
        record.setRoleId(roleId);
        record.setUserId(userId);
        record.setStyleName(dto.getStyleName());
        record.setSourceProfileUrl(dto.getReferenceImageUrl());
        record.setGenerateStatus(IMAGE_GENERATE_STATUS_PENDING);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        tsRoleImageGenerateRecordMapper.insert(record);
        return record;
    }

    private Long createSuccessImageGenerateRecord(String userId, Long roleId, TsRoleOneClickImageGenerateDto dto, ImageGenerateRuntimeResult runtime) {
        Date now = new Date();
        TsRoleImageGenerateRecord record = new TsRoleImageGenerateRecord();
        record.setRoleId(roleId);
        record.setUserId(userId);
        record.setPromptText(runtime.getImagePrompt());
        record.setStyleName(dto.getStyleName());
        record.setSourceProfileUrl(dto.getReferenceImageUrl());
        record.setGenerateStatus(IMAGE_GENERATE_STATUS_SUCCESS);
        record.setResultAssetId(runtime.getAssetId());
        record.setResultImageUrl(runtime.getImageUrl());
        record.setExtJson(runtime.getModelJson() == null ? null : runtime.getModelJson().toJSONString());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        tsRoleImageGenerateRecordMapper.insert(record);
        return record.getId();
    }

    private void markImageGenerateRunning(Long recordId) {
        TsRoleImageGenerateRecord update = new TsRoleImageGenerateRecord();
        update.setId(recordId);
        update.setGenerateStatus(IMAGE_GENERATE_STATUS_RUNNING);
        update.setUpdatedAt(new Date());
        tsRoleImageGenerateRecordMapper.updateById(update);
    }

    private void markImageGenerateSuccess(Long recordId, ImageGenerateRuntimeResult runtime) {
        TsRoleImageGenerateRecord update = new TsRoleImageGenerateRecord();
        update.setId(recordId);
        update.setPromptText(runtime.getImagePrompt());
        update.setGenerateStatus(IMAGE_GENERATE_STATUS_SUCCESS);
        update.setResultAssetId(runtime.getAssetId());
        update.setResultImageUrl(runtime.getImageUrl());
        update.setFailReason(null);
        update.setExtJson(runtime.getModelJson() == null ? null : runtime.getModelJson().toJSONString());
        update.setUpdatedAt(new Date());
        tsRoleImageGenerateRecordMapper.updateById(update);
    }

    private void markImageGenerateFailed(Long recordId, Exception ex) {
        TsRoleImageGenerateRecord update = new TsRoleImageGenerateRecord();
        update.setId(recordId);
        update.setGenerateStatus(IMAGE_GENERATE_STATUS_FAILED);
        update.setFailReason(trimFailureReason(ex));
        update.setUpdatedAt(new Date());
        tsRoleImageGenerateRecordMapper.updateById(update);
    }

    private void updateRoleAvatar(Long roleId, String imageUrl) {
        TsRole roleUpdate = new TsRole();
        roleUpdate.setId(roleId);
        roleUpdate.setAvatarUrl(imageUrl);
        roleUpdate.setUpdatedAt(new Date());
        tsRoleMapper.updateById(roleUpdate);
    }

    private String trimFailureReason(Exception ex) {
        String message = ex == null ? null : PromptRuntimeUtil.trimToNull(ex.getMessage());
        if (!StringUtils.hasText(message)) {
            return "形象生成失败";
        }
        if (message.length() <= IMAGE_FAIL_REASON_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, IMAGE_FAIL_REASON_MAX_LENGTH);
    }

    /**
     * 一键生成角色声音。
     * 可无条件生成；若传入角色设定字段则作为上下文参与推荐。
     */
    @Override
    public TsRoleOneClickVoiceGenerateVo generateRoleVoice(LoginUser user, TsRoleOneClickVoiceGenerateDto request) {
        // 请求归一化；若传 roleId 则做归属校验并加载角色实体。
        TsRoleOneClickVoiceGenerateDto dto = request == null ? new TsRoleOneClickVoiceGenerateDto() : request;
        dto.normalize();

        TsRole role = null;
        if (dto.getRoleId() != null) {
            role = tsRoleMapper.selectOwned(dto.getRoleId(), user.getId());
            if (role == null) {
                throw new JeecgBootException("角色不存在或无权访问");
            }
        }

        // 新策略：随机选择可用音色 + 随机语音参数，接口快速返回；试听音频由前端后续调用预览接口获取。
        String recommendedGender = PromptRuntimeUtil.normalizeGender(dto.getGender());
        TsVoiceProfile selected = selectRandomVoiceProfile(recommendedGender);
        if (selected == null) {
            throw new JeecgBootException("未找到可用音色，请先维护公共音色并确保 providerVoiceId 可用");
        }

        String previewText = PromptRuntimeUtil.firstNonBlank(dto.getPreviewText(), DEFAULT_PREVIEW_TEXT);
        Double speed = randomVoiceSpeed();
        Double pitch = randomVoicePitch();
        Double volume = randomVoiceVolume();
        String recommendation = "已按随机策略生成声音参数，请前端调用试听接口获取音频";
        String traceId = "role-voice-" + UUID.randomUUID().toString().replace("-", "");
        String schemaVersion = "v1";
        String matchSource = "random_pool";
        String previewAudioUrl = null;

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
        snapshot.put("promptRendered", null);
        snapshot.put("voiceProfileId", selected.getId());
        snapshot.put("voiceName", selected.getName());
        snapshot.put("previewAudioUrl", previewAudioUrl);
        snapshot.put("recommendedGender", recommendedGender);
        snapshot.put("selectionReason", recommendation);
        snapshot.put("speed", speed);
        snapshot.put("pitch", pitch);
        snapshot.put("volume", volume);
        snapshot.put("matchSource", matchSource);
        snapshot.put("traceId", traceId);
        snapshot.put("schemaVersion", schemaVersion);
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "voice", user.getId(), snapshot);

        // 组装响应给前端。
        TsRoleOneClickVoiceGenerateVo vo = new TsRoleOneClickVoiceGenerateVo();
        TsRoleOneClickVoiceGenerateVo.VoiceMeta voiceMeta = new TsRoleOneClickVoiceGenerateVo.VoiceMeta();
        voiceMeta.setVoiceName(selected.getName());
        voiceMeta.setVoiceGender(PromptRuntimeUtil.firstNonBlank(recommendedGender, "unknown"));
        voiceMeta.setVoiceProfileId(selected.getId());
        voiceMeta.setProviderVoiceId(selected.getProviderVoiceId());
        voiceMeta.setPreviewText(previewText);
        voiceMeta.setPreviewAudioUrl(previewAudioUrl);
        voiceMeta.setSpeed(speed);
        voiceMeta.setPitch(pitch);
        voiceMeta.setVolume(volume);
        voiceMeta.setSelectionReason(recommendation);
        voiceMeta.setMatchSource(matchSource);
        voiceMeta.setTraceId(traceId);
        voiceMeta.setSchemaVersion(schemaVersion);
        vo.setVoice(voiceMeta);
        vo.setVoiceProfileId(selected.getId());
        vo.setVoiceName(selected.getName());
        vo.setProviderVoiceId(selected.getProviderVoiceId());
        vo.setRecommendation(recommendation);
        vo.setPreviewText(previewText);
        vo.setPreviewAudioUrl(previewAudioUrl);
        vo.setSpeed(speed);
        vo.setPitch(pitch);
        vo.setVolume(volume);
        vo.setPromptCode(PROMPT_CODE_VOICE);
        vo.setPromptVersion(PROMPT_VERSION);
        vo.setRenderedPrompt(null);
        vo.setSnapshotKey(snapshotKey);
        vo.setMatchSource(matchSource);
        vo.setTraceId(traceId);
        vo.setSchemaVersion(schemaVersion);
        return vo;
    }

    private TsVoiceProfile selectRandomVoiceProfile(String preferredGender) {
        List<TsVoiceProfile> primaryProfiles = VoiceProfileMatchUtil.queryVoiceProfiles(tsVoiceProfileMapper, preferredGender, VOICE_CANDIDATE_LIMIT);
        List<TsVoiceProfile> fallbackProfiles = StringUtils.hasText(preferredGender)
                ? VoiceProfileMatchUtil.queryVoiceProfiles(tsVoiceProfileMapper, null, VOICE_CANDIDATE_LIMIT)
                : Collections.emptyList();
        List<TsVoiceProfile> candidates = new ArrayList<>();
        Set<Long> profileIds = new HashSet<>();
        appendVoiceCandidates(candidates, profileIds, primaryProfiles);
        appendVoiceCandidates(candidates, profileIds, fallbackProfiles);
        if (candidates.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }

    private void appendVoiceCandidates(List<TsVoiceProfile> target, Set<Long> profileIds, List<TsVoiceProfile> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (TsVoiceProfile profile : source) {
            if (profile == null || profile.getId() == null || !StringUtils.hasText(profile.getProviderVoiceId())) {
                continue;
            }
            if (profileIds.add(profile.getId())) {
                target.add(profile);
            }
        }
    }

    private Double randomVoiceSpeed() {
        return randomWithStep(0.8D, 1.2D, 0.1D);
    }

    private Double randomVoicePitch() {
        return (double) ThreadLocalRandom.current().nextInt(-6, 7);
    }

    private Double randomVoiceVolume() {
        return randomWithStep(0.8D, 1.2D, 0.1D);
    }

    private Double randomWithStep(double min, double max, double step) {
        int steps = (int) Math.round((max - min) / step);
        int pick = ThreadLocalRandom.current().nextInt(steps + 1);
        double value = min + pick * step;
        return Math.round(value * 10D) / 10D;
    }

    @Override
    public TsRoleGenerateTextByTemplateVo generateTextByTemplate(LoginUser user, TsRoleGenerateTextByTemplateDto request) {
        TsRoleGenerateTextByTemplateDto dto = request == null ? new TsRoleGenerateTextByTemplateDto() : request;
        dto.normalize();

        String promptCode = PromptRuntimeUtil.firstNonBlank(dto.getPromptCode(), PROMPT_CODE_TEXT_TEMPLATE);
        String promptVersion = PromptRuntimeUtil.firstNonBlank(dto.getPromptVersion(), PROMPT_VERSION);
        String promptPath = resolvePromptPath(promptCode, promptVersion);

        Map<String, String> variables = new LinkedHashMap<>();
        if (dto.getVariables() != null) {
            for (Map.Entry<String, Object> entry : dto.getVariables().entrySet()) {
                String key = PromptRuntimeUtil.trimToNull(entry.getKey());
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                Object rawValue = entry.getValue();
                String value = rawValue == null ? null : PromptRuntimeUtil.trimToNull(String.valueOf(rawValue));
                variables.put(key, PromptRuntimeUtil.nullableToken(value));
            }
        }

        String renderedPrompt = promptRenderService.renderPrompt(promptPath, variables);
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(promptChatService, renderedPrompt);
        String generatedText = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("generated_text")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("text")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("result")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("prompt_text"))
        );
        if (!StringUtils.hasText(generatedText)) {
            throw new JeecgBootException("模板生成文本失败，模型未返回有效文本");
        }

        JSONObject snapshot = new JSONObject();
        snapshot.put("type", "text-template");
        snapshot.put("promptCode", promptCode);
        snapshot.put("promptVersion", promptVersion);
        snapshot.put("promptRendered", renderedPrompt);
        snapshot.put("variables", variables);
        snapshot.put("rawResponse", modelJson == null ? null : modelJson.toJSONString());
        snapshot.put("result", generatedText);
        String snapshotKey = RoleGenerateSnapshotUtil.saveSnapshot(redisTemplate, REDIS_SNAPSHOT_PREFIX, REDIS_SNAPSHOT_TTL_HOURS,
                "text-template", user.getId(), snapshot);

        TsRoleGenerateTextByTemplateVo vo = new TsRoleGenerateTextByTemplateVo();
        vo.setGeneratedText(generatedText);
        vo.setPromptCode(promptCode);
        vo.setPromptVersion(promptVersion);
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
        JSONObject modelJson = PromptRuntimeUtil.callPromptChat(promptChatService, renderedPrompt);

        // 抽取模型结果；若模型未返回字段则保持 null 语义，由后续链路自行处理。
        String roleName = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("role_name")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("roleName"))
        );
        String gender = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.normalizeGender(modelJson.getString("gender"))
        );
        String occupation = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("occupation")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("job"))
        );
        String backgroundStory = PromptRuntimeUtil.firstNonBlank(
                PromptRuntimeUtil.trimToNull(modelJson.getString("background_story")),
                PromptRuntimeUtil.trimToNull(modelJson.getString("backgroundStory"))
        );
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

    private String resolvePromptPath(String promptCode, String promptVersion) {
        if (PROMPT_CODE_TEXT_TEMPLATE.equals(promptCode) && PROMPT_VERSION.equals(promptVersion)) {
            return PROMPT_PATH_TEXT_TEMPLATE;
        }
        throw new JeecgBootException("不支持的模板编码或版本: " + promptCode + "@" + promptVersion);
    }

}
