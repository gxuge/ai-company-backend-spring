package org.jeecg.modules.system.agent.task.story;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryGenerateCompleteToolContract;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.system.dto.tsrole.TsRoleConfirmedGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneImageGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryRoleBindingDto;
import org.jeecg.modules.system.dto.tsstory.TsStorySaveDto;
import org.jeecg.modules.system.dto.tsuserimageasset.TsUserImageAssetImportDto;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.service.ITsStoryService;
import org.jeecg.modules.system.service.ITsUserImageAssetService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneImageGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryVo;
import org.jeecg.modules.system.vo.tsuserimageasset.TsUserImageAssetVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 完整故事后台生成服务。
 */
@Service
@RequiredArgsConstructor
public class StoryGenerateCompleteAsyncService {
    private static final String DEFAULT_NODE_NAME = "story_create_dialog";

    private final ITsRoleGenerateService roleGenerateService;
    private final ITsStoryGenerateService storyGenerateService;
    private final ITsStoryService storyService;
    private final ITsUserImageAssetService userImageAssetService;
    private final AgentEventPublisher eventPublisher;

    /**
     * 提交完整故事生成任务并立即返回受理结果。
     */
    public ToolCallResult submit(AgentContext context,
                                 ToolCallRequest request,
                                 Map<String, Object> transferData) {
        String taskId = request == null ? null : request.getTaskId();
        String eventId = request == null ? null : request.getEventId();
        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(eventId)) {
            throw new IllegalArgumentException("异步故事生成缺少 taskId 或 eventId");
        }

        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        AgentContext asyncContext = context == null ? new AgentContext() : context.fork(context.getUserInput());
        String nodeName = context == null || context.getCurrentNodeName() == null
                ? DEFAULT_NODE_NAME
                : context.getCurrentNodeName();

        CompletableFuture.runAsync(() -> executeGeneration(
                asyncContext,
                user,
                taskId,
                eventId,
                nodeName,
                transferData
        ));

        String transferDataJson = JSON.toJSONString(transferData);
        StoryGenerateCompleteToolContract.markAccepted(context, taskId, eventId, transferDataJson);

        Map<String, Object> accepted = new LinkedHashMap<>();
        accepted.put("taskId", taskId);
        accepted.put("eventId", eventId);
        accepted.put("status", "running");
        ToolCallResult result = ToolCallResult.asyncAccepted(
                "完整故事生成任务已开始",
                taskId,
                eventId,
                accepted
        );
        result.setPayload(new LinkedHashMap<>(accepted));
        return result;
    }

    private void executeGeneration(AgentContext context,
                                   LoginUser user,
                                   String taskId,
                                   String eventId,
                                   String nodeName,
                                   Map<String, Object> transferData) {
        try {
            StoryGenerationInput generationInput = readGenerationInput(transferData);
            List<Map<String, Object>> roleDataList = generationInput.roles();
            CompletableFuture<TsStoryOneClickSceneImageGenerateVo> sceneImageFuture =
                    CompletableFuture.supplyAsync(() -> generateStorySceneImage(user, generationInput));
            List<CompletableFuture<TsRoleGenerateRoleVo>> roleFutures = new ArrayList<>(roleDataList.size());
            for (Map<String, Object> roleData : roleDataList) {
                roleFutures.add(CompletableFuture.supplyAsync(() -> generateRole(user, roleData)));
            }

            List<TsRoleGenerateRoleVo> roleResults = new ArrayList<>(roleFutures.size());
            for (CompletableFuture<TsRoleGenerateRoleVo> roleFuture : roleFutures) {
                roleResults.add(roleFuture.join());
            }
            TsStoryOneClickSceneImageGenerateVo sceneImageResult = sceneImageFuture.join();
            TsStoryVo storyResult = saveStory(user, generationInput, roleResults, sceneImageResult);

            Map<String, Object> toolData = new LinkedHashMap<>();
            toolData.put("storyId", storyResult == null ? null : storyResult.getId());
            toolData.put("story", storyResult);
            toolData.put("roles", roleResults);
            toolData.put("sceneImage", sceneImageResult);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("taskId", taskId);
            payload.put("eventId", eventId);
            payload.put("async", Boolean.TRUE);
            payload.put(
                    "toolArguments",
                    Map.of(StoryGenerateCompleteToolContract.TRANSFER_DATA, transferData)
            );
            payload.put("toolData", toolData);
            this.eventPublisher.publishAsyncToolEnd(
                    context,
                    eventId,
                    nodeName,
                    StoryTaskToolSpec.STORY_GENERATE_COMPLETE,
                    "完整故事生成完成",
                    payload
            );
        } catch (Exception ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("taskId", taskId);
            payload.put("eventId", eventId);
            payload.put("async", Boolean.TRUE);
            payload.put(
                    "toolArguments",
                    Map.of(StoryGenerateCompleteToolContract.TRANSFER_DATA, transferData)
            );
            this.eventPublisher.publishAsyncToolError(
                    context,
                    eventId,
                    nodeName,
                    StoryTaskToolSpec.STORY_GENERATE_COMPLETE,
                    unwrap(ex),
                    payload
            );
        }
    }

    private TsRoleGenerateRoleVo generateRole(LoginUser user, Map<String, Object> roleData) {
        TsRoleConfirmedGenerateDto request = new TsRoleConfirmedGenerateDto();
        request.setRoleName(stringValue(roleData.get("roleName")));
        request.setGender(stringValue(roleData.get("gender")));
        request.setOccupation(stringValue(roleData.get("occupation")));
        request.setBackgroundStory(stringValue(roleData.get("backgroundStory")));
        request.normalize();
        return this.roleGenerateService.generateConfirmedRole(user, request);
    }

    private TsStoryOneClickSceneImageGenerateVo generateStorySceneImage(
            LoginUser user, StoryGenerationInput input) {
        TsStoryOneClickSceneImageGenerateDto imageRequest = new TsStoryOneClickSceneImageGenerateDto();
        imageRequest.setTitle(input.title());
        imageRequest.setStorySetting(buildStorySettingWithRoles(input.storySetting(), input.roles()));
        imageRequest.setSiteSetting(input.siteSetting());
        imageRequest.setPlotOutline(input.plotOutline());
        TsStoryOneClickSceneImageGenerateVo sceneImageResult =
                this.storyGenerateService.generateStorySceneImage(user, imageRequest);
        String persistedImageUrl = persistSceneImage(user, sceneImageResult);
        sceneImageResult.setImageUrl(persistedImageUrl);
        return sceneImageResult;
    }

    private TsStoryVo saveStory(LoginUser user,
                                StoryGenerationInput input,
                                List<TsRoleGenerateRoleVo> roleResults,
                                TsStoryOneClickSceneImageGenerateVo sceneImageResult) {
        TsStorySaveDto request = new TsStorySaveDto();
        request.setTitle(input.title());
        request.setSiteSetting(input.siteSetting());
        request.setStoryBackground(input.storySetting());
        request.setPlotOutline(input.plotOutline());
        request.setSceneImageUrl(sceneImageResult == null ? null : sceneImageResult.getImageUrl());
        request.setStatus(0);
        request.setIsPublic(0);
        request.setIsAiStorySetting(1);
        request.setIsAiCharacter(1);
        request.setIsAiOutline(1);
        request.setRoleBindings(buildRoleBindings(roleResults));

        Result<TsStoryVo> result = this.storyService.addStory(user, request);
        if (result == null || !result.isSuccess() || result.getResult() == null) {
            throw new IllegalStateException(result == null ? "故事保存失败" : result.getMessage());
        }
        return result.getResult();
    }

    private String persistSceneImage(LoginUser user, TsStoryOneClickSceneImageGenerateVo imageResult) {
        if (imageResult == null || !StringUtils.hasText(imageResult.getImageUrl())) {
            throw new IllegalStateException("故事场景背景图片生成失败，未返回图片地址");
        }
        TsUserImageAssetImportDto importRequest = new TsUserImageAssetImportDto();
        importRequest.setSourceImageUrl(imageResult.getImageUrl());
        importRequest.setFileName("story-scene-" + System.currentTimeMillis() + ".png");
        importRequest.setSourceType("ai_generate");
        Result<TsUserImageAssetVo> assetResult = this.userImageAssetService.importAsset(user, importRequest);
        TsUserImageAssetVo asset = assetResult == null ? null : assetResult.getResult();
        if (asset == null || !StringUtils.hasText(asset.getFileUrl())) {
            throw new IllegalStateException("故事场景背景图片保存失败，未返回永久图片地址");
        }
        return asset.getFileUrl().trim();
    }

    private List<TsStoryRoleBindingDto> buildRoleBindings(List<TsRoleGenerateRoleVo> roleResults) {
        List<TsStoryRoleBindingDto> bindings = new ArrayList<>(roleResults.size());
        for (int index = 0; index < roleResults.size(); index++) {
            TsRoleGenerateRoleVo roleResult = roleResults.get(index);
            if (roleResult == null || roleResult.getRoleId() == null) {
                throw new IllegalStateException("角色生成完成但未返回角色ID");
            }
            TsStoryRoleBindingDto binding = new TsStoryRoleBindingDto();
            binding.setRoleId(roleResult.getRoleId());
            binding.setRoleType(index == 0 ? "lead" : "support");
            binding.setSortNo(index);
            binding.setIsRequired(1);
            binding.setJoinSource("ai_generate");
            bindings.add(binding);
        }
        return bindings;
    }

    private StoryGenerationInput readGenerationInput(Map<String, Object> transferData) {
        Object roles = transferData == null ? null : transferData.get(StoryGenerateCompleteToolContract.ROLES);
        if (!(roles instanceof List<?> roleList) || roleList.isEmpty()) {
            throw new IllegalArgumentException("transferData.roles 不能为空");
        }
        List<Map<String, Object>> normalizedRoles = new ArrayList<>(roleList.size());
        for (Object role : roleList) {
            if (!(role instanceof Map<?, ?> roleMap)) {
                throw new IllegalArgumentException("transferData.roles 中存在无效角色数据");
            }
            Map<String, Object> normalizedRole = new LinkedHashMap<>();
            roleMap.forEach((key, value) -> normalizedRole.put(String.valueOf(key), value));
            normalizedRoles.add(normalizedRole);
        }
        return new StoryGenerationInput(
                stringValue(transferData.get("title")),
                stringValue(transferData.get("storySetting")),
                stringValue(transferData.get("siteSetting")),
                stringValue(transferData.get("plotOutline")),
                List.copyOf(normalizedRoles)
        );
    }

    private String buildStorySettingWithRoles(String storySetting, List<Map<String, Object>> roles) {
        String roleProfiles = JSON.toJSONString(roles);
        if (!StringUtils.hasText(storySetting)) {
            return "角色列表：\n" + roleProfiles;
        }
        return storySetting.trim() + "\n\n角色列表：\n" + roleProfiles;
    }

    private Throwable unwrap(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null
                && (cause instanceof java.util.concurrent.CompletionException
                || cause instanceof java.util.concurrent.ExecutionException)) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record StoryGenerationInput(String title,
                                        String storySetting,
                                        String siteSetting,
                                        String plotOutline,
                                        List<Map<String, Object>> roles) {
    }
}
