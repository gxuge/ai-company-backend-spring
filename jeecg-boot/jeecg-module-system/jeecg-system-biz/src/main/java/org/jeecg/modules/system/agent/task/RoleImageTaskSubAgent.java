package org.jeecg.modules.system.agent.task;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 角色图片任务子 Agent。
 *
 * @author codex
 * @date 2026/7/1
 */
@Component
public class RoleImageTaskSubAgent implements SubAgent {

    /**
     * 角色图片生成服务。
     */
    private final ITsRoleGenerateService roleGenerateService;

    /**
     * 构造函数。
     *
     * @param roleGenerateService 角色图片生成服务
     */
    public RoleImageTaskSubAgent(ITsRoleGenerateService roleGenerateService) {
        this.roleGenerateService = roleGenerateService;
    }

    @Override
    public String subAgentName() {
        return "role_image_task_agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, String> promptVariables = TaskImageSupport.readPromptVariables(context);
        JSONObject memory = TaskImageSupport.readSessionMemory(context);
        Map<String, Object> routeDecision = TaskAgentSupport.readMapAttribute(context, "routeDecision");
        String userInput = TaskAgentSupport.normalizeText(context == null ? null : context.getUserInput());
        String taskGoal = TaskAgentSupport.normalizeText(stringValue(routeDecision.get("taskGoal")));

        TsRoleOneClickImageGenerateDto request = new TsRoleOneClickImageGenerateDto();
        request.setRoleId(TaskImageSupport.resolveLong(promptVariables, memory, "roleId", "role_id"));
        request.setRoleName(TaskImageSupport.resolveText(promptVariables, memory,
                PromptRuntimeUtil.firstNonBlank(taskGoal, userInput),
                "role_name", "roleName"));
        request.setGender(PromptRuntimeUtil.normalizeGender(TaskImageSupport.resolveText(promptVariables, memory,
                null,
                "gender")));
        request.setOccupation(TaskImageSupport.resolveText(promptVariables, memory,
                PromptRuntimeUtil.firstNonBlank(userInput, taskGoal),
                "occupation"));
        request.setBackgroundStory(TaskImageSupport.resolveText(promptVariables, memory,
                PromptRuntimeUtil.firstNonBlank(userInput, taskGoal),
                "background_story", "backgroundStory"));
        request.setStyleName(TaskImageSupport.resolveText(promptVariables, memory, null, "style_name", "styleName"));
        request.setAspectRatio(TaskImageSupport.resolveText(promptVariables, memory, null, "aspect_ratio", "aspectRatio"));
        request.setReferenceImageUrl(TaskImageSupport.resolveText(promptVariables, memory, null, "reference_image_url", "referenceImageUrl"));
        request.setAsyncGenerate(Boolean.FALSE);
        request.normalize();

        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsRoleOneClickImageGenerateVo vo = this.roleGenerateService.generateRoleImage(user, request);
        return buildResult(vo, request);
    }

    /**
     * 组装角色图片结果。
     */
    private AgentResult buildResult(TsRoleOneClickImageGenerateVo vo, TsRoleOneClickImageGenerateDto request) {
        AgentResult result = AgentResult.success("已生成角色图片");
        result.getData().put("subAgentName", subAgentName());
        result.getData().put("executionMode", "image");
        result.getData().put("taskType", "role_image");
        result.getData().put("result", vo);
        result.getData().put("resultJson", JSONObject.toJSONString(vo));
        result.getData().put("roleId", request.getRoleId());
        result.getData().put("roleName", request.getRoleName());
        result.getData().put("gender", request.getGender());
        result.getData().put("occupation", request.getOccupation());
        result.getData().put("backgroundStory", request.getBackgroundStory());
        result.getData().put("imageUrl", vo == null ? null : vo.getImageUrl());
        result.getData().put("imagePrompt", vo == null ? null : vo.getImagePrompt());
        result.getData().put("promptCode", vo == null ? null : vo.getPromptCode());
        result.getData().put("promptVersion", vo == null ? null : vo.getPromptVersion());
        result.getData().put("renderedPrompt", vo == null ? null : vo.getRenderedPrompt());
        result.getData().put("generateRecordId", vo == null ? null : vo.getGenerateRecordId());
        result.getData().put("accepted", vo != null && Boolean.TRUE.equals(vo.getAccepted()));
        result.getData().put("generateStatus", vo == null ? null : vo.getGenerateStatus());
        result.getData().put("snapshotKey", vo == null ? null : vo.getSnapshotKey());
        return result;
    }

    /**
     * 将对象值转为文本。
     */
    private static String stringValue(Object value) {
        return value == null ? null : TaskAgentSupport.normalizeText(String.valueOf(value));
    }
}
