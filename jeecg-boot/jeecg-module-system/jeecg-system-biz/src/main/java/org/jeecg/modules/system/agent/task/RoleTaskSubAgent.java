package org.jeecg.modules.system.agent.task;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色任务子 Agent。
 *
 * @author codex
 * @date 2026/7/1
 */
@Component
public class RoleTaskSubAgent implements SubAgent {
    /**
     * 角色生成服务。
     */
    private final ITsRoleGenerateService roleGenerateService;
    /**
     * Agent 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;

    /**
     * 构造函数。
     *
     * @param roleGenerateService 角色生成服务
     * @param eventPublisher 事件发布器
     */
    public RoleTaskSubAgent(ITsRoleGenerateService roleGenerateService, AgentEventPublisher eventPublisher) {
        this.roleGenerateService = roleGenerateService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String subAgentName() {
        return "role_task_agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String userInput = TaskAgentSupport.normalizeText(context == null ? null : context.getUserInput());
        boolean usePreset = TaskAgentSupport.isVagueRoleRequest(userInput);
        String executionMode = usePreset ? "preset" : "full";
        Map<String, Object> requestSummary = buildRequestSummary(context, userInput, executionMode);
        this.eventPublisher.publishSubAgentStart(context, subAgentName(), requestSummary);
        try {
            if (usePreset) {
                TsRoleOneClickSettingGenerateDto request = new TsRoleOneClickSettingGenerateDto();
                request.setBackgroundStory(userInput);
                request.setStyleHint(userInput);
                request.setKeywords(userInput);
                request.normalize();
                TsRoleOneClickSettingGenerateVo vo = this.roleGenerateService.generateRoleSettingPreset(TaskAgentSupport.buildLoginUser(context), request);
                AgentResult result = buildResult("已生成角色核心设定", executionMode, requestSummary, vo);
                this.eventPublisher.publishSubAgentEnd(context, subAgentName(), result, requestSummary);
                return result;
            }

            TsRoleGenerateRoleDto request = new TsRoleGenerateRoleDto();
            request.setStorySetting(userInput);
            request.setStoryBackground(userInput);
            request.normalize();
            TsRoleGenerateRoleVo vo = this.roleGenerateService.generateRole(TaskAgentSupport.buildLoginUser(context), request);
            AgentResult result = buildResult("已生成完整角色", executionMode, requestSummary, vo);
            this.eventPublisher.publishSubAgentEnd(context, subAgentName(), result, requestSummary);
            return result;
        } catch (Exception ex) {
            this.eventPublisher.publishSubAgentError(context, subAgentName(), ex, buildErrorPayload(context, userInput, executionMode));
            AgentResult failed = AgentResult.failed(ex.getMessage());
            failed.getData().put("subAgentName", subAgentName());
            failed.getData().put("executionMode", executionMode);
            failed.getData().put("request", requestSummary);
            failed.getData().put("errorMessage", ex.getMessage());
            return failed;
        }
    }

    /**
     * 组装任务结果。
     *
     * @param summary 返回说明
     * @param mode 执行模式
     * @param result 生成结果
     * @return Agent 结果
     */
    private AgentResult buildResult(String summary, String mode, Map<String, Object> requestSummary, Object result) {
        AgentResult agentResult = AgentResult.success(summary);
        agentResult.getData().put("subAgentName", subAgentName());
        agentResult.getData().put("executionMode", mode);
        agentResult.getData().put("request", requestSummary);
        agentResult.getData().put("result", result);
        agentResult.getData().put("resultJson", JSONObject.toJSONString(result));
        return agentResult;
    }

    /**
     * 组装事件载荷。
     *
     * @param context 运行上下文
     * @param userInput 用户输入
     * @param executionMode 执行模式
     * @return 事件载荷
     */
    private Map<String, Object> buildRequestSummary(AgentContext context, String userInput, String executionMode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subAgentName", subAgentName());
        payload.put("executionMode", executionMode);
        payload.put("request", userInput);
        payload.put("usePreset", "preset".equalsIgnoreCase(executionMode));
        payload.put("intentMode", oConvertUtils.getString(context == null ? null : context.getAttribute("intentMode")));
        payload.put("targetAgent", oConvertUtils.getString(context == null ? null : context.getAttribute("targetAgent")));
        payload.put("taskGoal", oConvertUtils.getString(context == null ? null : context.getAttribute("taskGoal")));
        return payload;
    }

    /**
     * 组装错误载荷。
     *
     * @param userInput 用户输入
     * @param executionMode 执行模式
     * @return 错误载荷
     */
    private Map<String, Object> buildErrorPayload(AgentContext context, String userInput, String executionMode) {
        Map<String, Object> payload = buildRequestSummary(context, userInput, executionMode);
        payload.put("stage", "role_generate");
        return payload;
    }
}
