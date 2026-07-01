package org.jeecg.modules.system.agent.task;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGenerateVo;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事任务子 Agent。
 *
 * @author codex
 * @date 2026/7/1
 */
@Component
public class StoryTaskSubAgent implements SubAgent {
    /**
     * 故事生成服务。
     */
    private final ITsStoryGenerateService storyGenerateService;
    /**
     * Agent 事件发布器。
     */
    private final AgentEventPublisher eventPublisher;

    /**
     * 构造函数。
     *
     * @param storyGenerateService 故事生成服务
     * @param eventPublisher 事件发布器
     */
    public StoryTaskSubAgent(ITsStoryGenerateService storyGenerateService, AgentEventPublisher eventPublisher) {
        this.storyGenerateService = storyGenerateService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String subAgentName() {
        return "story_task_agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String userInput = TaskAgentSupport.normalizeText(context == null ? null : context.getUserInput());
        boolean usePreset = TaskAgentSupport.isVagueStoryRequest(userInput);
        TsStoryFullGenerateDto request = buildRequest(userInput);
        String mode;
        if (usePreset) {
            mode = "preset";
        } else {
            mode = "full";
        }
        Map<String, Object> requestSummary = buildRequestSummary(context, userInput, mode, request);
        this.eventPublisher.publishSubAgentStart(context, subAgentName(), requestSummary);
        try {
            TsStoryFullGenerateVo vo;
            if (usePreset) {
                vo = this.storyGenerateService.generateStoryFullPreset(TaskAgentSupport.buildLoginUser(context), request);
            } else {
                vo = this.storyGenerateService.generateStoryFull(TaskAgentSupport.buildLoginUser(context), request);
            }
            AgentResult result = buildResult(usePreset ? "已生成故事 preset" : "已生成完整故事", mode, requestSummary, vo);
            this.eventPublisher.publishSubAgentEnd(context, subAgentName(), result, requestSummary);
            return result;
        } catch (Exception ex) {
            this.eventPublisher.publishSubAgentError(context, subAgentName(), ex, buildErrorPayload(context, userInput, mode, request));
            AgentResult failed = AgentResult.failed(ex.getMessage());
            failed.getData().put("subAgentName", subAgentName());
            failed.getData().put("executionMode", mode);
            failed.getData().put("request", requestSummary);
            failed.getData().put("errorMessage", ex.getMessage());
            return failed;
        }
    }

    /**
     * 构造故事请求。
     *
     * @param userInput 用户输入
     * @return 故事请求
     */
    private TsStoryFullGenerateDto buildRequest(String userInput) {
        TsStoryFullGenerateDto request = new TsStoryFullGenerateDto();
        request.setTitle(userInput);
        request.setStoryIntro(userInput);
        request.setStorySetting(userInput);
        request.setSiteSetting(userInput);
        request.setPlotOutline(userInput);
        request.normalize();
        return request;
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
     * 组装请求摘要。
     *
     * @param context 运行上下文
     * @param userInput 用户输入
     * @param executionMode 执行模式
     * @param request 请求对象
     * @return 请求摘要
     */
    private Map<String, Object> buildRequestSummary(AgentContext context, String userInput, String executionMode, TsStoryFullGenerateDto request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subAgentName", subAgentName());
        payload.put("executionMode", executionMode);
        payload.put("request", request == null ? userInput : request);
        payload.put("usePreset", "preset".equalsIgnoreCase(executionMode));
        payload.put("intentMode", context == null ? null : context.getAttribute("intentMode"));
        payload.put("targetAgent", context == null ? null : context.getAttribute("targetAgent"));
        payload.put("taskGoal", context == null ? null : context.getAttribute("taskGoal"));
        return payload;
    }

    /**
     * 组装错误载荷。
     *
     * @param context 运行上下文
     * @param userInput 用户输入
     * @param executionMode 执行模式
     * @param request 请求对象
     * @return 错误载荷
     */
    private Map<String, Object> buildErrorPayload(AgentContext context, String userInput, String executionMode, TsStoryFullGenerateDto request) {
        Map<String, Object> payload = buildRequestSummary(context, userInput, executionMode, request);
        payload.put("stage", "story_generate");
        return payload;
    }
}
