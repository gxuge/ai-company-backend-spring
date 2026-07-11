package org.jeecg.modules.system.agent.task.story;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.common.SubAgentHistorySupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事任务真实工具注册器。
 *
 * <p>放在 system-biz 侧作为业务适配器，负责把 Agent 工具调用转发到现有故事生成服务。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
@Component
public class StoryTaskToolRegistrar {

    private static final String ROUTE_STORY_CORE_FILL = "STORY_CORE_FILL";
    private static final String ROUTE_STORY_FULL_GENERATE = "STORY_FULL_GENERATE";

    private final ToolRegistry toolRegistry;
    private final ITsStoryGenerateService storyGenerateService;

    public StoryTaskToolRegistrar(ToolRegistry toolRegistry,
                                  ITsStoryGenerateService storyGenerateService) {
        this.toolRegistry = toolRegistry;
        this.storyGenerateService = storyGenerateService;
    }

    @PostConstruct
    public void registerTools() {
        this.toolRegistry.register(buildStoryFullGenerateTool());
        this.toolRegistry.register(buildStoryFullGeneratePresetTool());
    }

    private ToolDefinition buildStoryFullGenerateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_FULL_GENERATE);
        definition.setRouteKey(ROUTE_STORY_FULL_GENERATE);
        definition.setCategory("story_task");
        definition.setDisplayName("完整故事生成");
        definition.setDescription("适合故事信息较完整、直接生成完整故事时使用");
        definition.setExecutor(this::executeStoryFullGenerate);
        return definition;
    }

    private ToolDefinition buildStoryFullGeneratePresetTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET);
        definition.setRouteKey(ROUTE_STORY_CORE_FILL);
        definition.setCategory("story_task");
        definition.setDisplayName("故事核心字段补全");
        definition.setDescription("适合故事信息较少、需要先补全核心设定时使用");
        definition.setExecutor(this::executeStoryFullGeneratePreset);
        return definition;
    }

    private ToolCallResult executeStoryFullGenerate(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryFullGenerateDto dto = buildStoryRequest(context, request);
        Map<String, Object> payload = buildStoryPayload(context, request, "full", StoryTaskToolSpec.STORY_FULL_GENERATE);
        Object result = this.storyGenerateService.generateStoryFull(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        ToolCallResult callResult = ToolCallResult.success("已生成完整故事", result);
        callResult.setPayload(payload);
        return callResult;
    }

    private ToolCallResult executeStoryFullGeneratePreset(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryFullGenerateDto dto = buildStoryRequest(context, request);
        Map<String, Object> payload = buildStoryPayload(context, request, "preset", StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET);
        Object result = this.storyGenerateService.generateStoryFullPreset(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        ToolCallResult callResult = ToolCallResult.success("已生成故事 preset", result);
        callResult.setPayload(payload);
        return callResult;
    }

    private TsStoryFullGenerateDto buildStoryRequest(AgentContext context, ToolCallRequest toolRequest) {
        TsStoryFullGenerateDto dto = new TsStoryFullGenerateDto();
        Map<String, Object> args = toolRequest == null ? null : toolRequest.getArguments();
        Map<String, Object> promptVariables = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        dto.setTitle(firstText(args, promptVariables, "title"));
        dto.setStoryMode(firstText(args, promptVariables, "storyMode", "story_mode"));
        dto.setStoryIntro(firstText(args, promptVariables, "storyIntro", "story_intro"));
        dto.setStorySetting(firstText(args, promptVariables, "storySetting", "story_setting"));
        dto.setSiteSetting(firstText(args, promptVariables, "siteSetting", "site_setting"));
        dto.setPlotOutline(firstText(args, promptVariables, "plotOutline", "plot_outline"));
        dto.normalize();
        return dto;
    }

    private Map<String, Object> buildStoryPayload(AgentContext context, ToolCallRequest request, String executionMode, String toolName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolName", toolName);
        payload.put("executionMode", executionMode);
        if (request != null) {
            payload.put("arguments", request.getArguments());
        }
        String historyJson = TaskAgentSupport.readStringAttribute(context, "subAgentHistoryJson");
        payload.put("historyCount", SubAgentHistorySupport.countHistory(historyJson));
        payload.put("historyBlock", context == null ? null : context.getAttribute("subAgentHistoryBlock"));
        return payload;
    }

    private String firstText(Map<String, Object> firstSource, Map<String, Object> secondSource, String... keys) {
        String value = firstText(firstSource, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return firstText(secondSource, keys);
    }

    private String firstText(Map<String, Object> source, String... keys) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            Object value = source.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }
}
