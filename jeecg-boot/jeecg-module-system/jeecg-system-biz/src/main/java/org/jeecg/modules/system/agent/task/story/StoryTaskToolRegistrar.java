package org.jeecg.modules.system.agent.task.story;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.common.SubAgentHistorySupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
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
    private static final String ROUTE_STORY_CONFIRMATION_DECISION = "STORY_CONFIRMATION_DECISION";
    private static final String ROUTE_STORY_FLOW_GATE = "STORY_FLOW_GATE";
    private static final String ROUTE_STORY_SCENE_GENERATE = "STORY_SCENE_GENERATE";

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
        this.toolRegistry.register(buildStoryConfirmationDecisionTool());
        this.toolRegistry.register(buildStoryFlowGateTool());
        this.toolRegistry.register(buildStorySceneGenerateTool());
    }

    private ToolDefinition buildStoryFullGenerateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_FULL_GENERATE);
        definition.setRouteKey(ROUTE_STORY_FULL_GENERATE);
        definition.setCategory("story_task");
        definition.setDisplayName("完整故事生成");
        definition.setDescription("适合故事信息较完整、直接生成完整故事时使用；可通过extraInfo补充不属于核心字段的故事信息");
        definition.setInputSchema("""
                {
                  "type":"object",
                  "properties":{
                    "userInput":{"type":"string","description":"用户原始输入或本次任务描述；未显式传extraInfo时可作为额外信息"},
                    "title":{"type":"string","description":"故事标题，可为空"},
                    "storyMode":{"type":"string","description":"故事模式，可为空","enum":["normal","chapter"]},
                    "storyIntro":{"type":"string","description":"故事简介，可为空"},
                    "storySetting":{"type":"string","description":"故事设定，可为空"},
                    "siteSetting":{"type":"string","description":"场景设定，可为空"},
                    "plotOutline":{"type":"string","description":"剧情大纲，可为空"},
                    "extraInfo":{"type":"string","description":"额外信息；用于补充故事信息；可选，null或空白时忽略；执行器同时兼容extra_info"}
                  },
                  "additionalProperties":true
                }
                """);
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

    private ToolDefinition buildStoryConfirmationDecisionTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_CONFIRMATION_DECISION);
        definition.setRouteKey(ROUTE_STORY_CONFIRMATION_DECISION);
        definition.setCategory("story_task");
        definition.setDisplayName("故事确认判断");
        definition.setDescription("已有故事核心设定后，由LLM判断用户是接受继续、重新生成、修改，还是需要展示选择");
        definition.setExecutor(this::executeStoryConfirmationDecision);
        return definition;
    }

    private ToolDefinition buildStoryFlowGateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_FLOW_GATE);
        definition.setRouteKey(ROUTE_STORY_FLOW_GATE);
        definition.setCategory("story_task");
        definition.setDisplayName("故事流程门禁");
        definition.setDescription("判断故事核心结果是否可以进入背景 / 场景阶段");
        definition.setExecutor(this::executeStoryFlowGate);
        return definition;
    }

    private ToolDefinition buildStorySceneGenerateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_GENERATE_SCENE);
        definition.setRouteKey(ROUTE_STORY_SCENE_GENERATE);
        definition.setCategory("story_task");
        definition.setDisplayName("故事背景生成");
        definition.setDescription("基于已确认的故事核心生成背景 / 场景设定");
        definition.setExecutor(this::executeStorySceneGenerate);
        return definition;
    }

    private ToolCallResult executeStoryFullGenerate(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryFullGenerateDto dto = buildStoryRequest(context, request);
        Map<String, Object> payload = buildStoryPayload(context, request, "full", StoryTaskToolSpec.STORY_FULL_GENERATE);
        Object result = this.storyGenerateService.generateStoryFull(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("storyFullGenerateResult", result);
            context.putAttribute("storyFullGenerateResultJson", resultJson);
            context.putAttribute("storyCoreResult", result);
            context.putAttribute("storyCoreResultJson", resultJson);
            context.putAttribute("storyFlowStage", "story_confirm");
        }
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
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("storyCorePresetResult", result);
            context.putAttribute("storyCorePresetResultJson", resultJson);
            context.putAttribute("storyCoreResult", result);
            context.putAttribute("storyCoreResultJson", resultJson);
            context.putAttribute("storyFlowStage", "story_confirm");
        }
        ToolCallResult callResult = ToolCallResult.success("已生成故事 preset", result);
        callResult.setPayload(payload);
        return callResult;
    }

    private ToolCallResult executeStoryConfirmationDecision(AgentContext context, ToolCallRequest request) {
        Map<String, Object> args = request == null ? null : request.getArguments();
        String action = normalizeConfirmationAction(firstText(args, "action"));
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("action", action);
        decision.put("reply", firstTextWithDefault(args, buildConfirmationReply(action), "reply"));
        decision.put("options", normalizeOptions(args == null ? null : args.get("options")));
        decision.put("reason", firstTextWithDefault(args, buildConfirmationReason(action), "reason"));
        if (context != null) {
            context.putAttribute("storyConfirmationDecision", decision);
            context.putAttribute("storyConfirmationDecisionJson", JSONObject.toJSONString(decision));
        }
        ToolCallResult callResult = ToolCallResult.success("已接收故事确认判断", decision);
        Map<String, Object> payload = buildStoryPayload(context, request, "confirmation", StoryTaskToolSpec.STORY_CONFIRMATION_DECISION);
        payload.put("decision", decision);
        callResult.setPayload(payload);
        return callResult;
    }

    private ToolCallResult executeStoryFlowGate(AgentContext context, ToolCallRequest request) {
        String stage = firstNonBlank(context, request, "stage");
        if (!StringUtils.hasText(stage) && context != null) {
            stage = oConvertUtils.getString(context.getAttribute("storyFlowStage"));
        }
        if (!StringUtils.hasText(stage) && hasStoryCoreState(context)) {
            stage = "story_confirm";
        }
        boolean shouldWait = "story_confirm".equalsIgnoreCase(stage)
                || "core".equalsIgnoreCase(stage)
                || "preset".equalsIgnoreCase(stage);
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("action", shouldWait ? "WAIT_CONFIRM" : "NEXT");
        decision.put("question", shouldWait ? "你对这版故事满意吗？" : null);
        decision.put("options", shouldWait
                ? List.of("满意，继续生成", "不满意，重新生成")
                : List.of());
        decision.put("reason", shouldWait ? "故事设定已生成，等待用户选择是否继续" : "当前阶段可继续");
        if (context != null) {
            context.putAttribute("storyFlowGateDecision", decision);
            context.putAttribute("storyFlowGateDecisionJson", JSONObject.toJSONString(decision));
            context.putAttribute("storyFlowStage", shouldWait ? "story_confirm" : "done");
        }
        ToolCallResult callResult = ToolCallResult.success(shouldWait ? "需要用户确认" : "可以继续下一步", decision);
        Map<String, Object> payload = buildStoryPayload(context, request, "gate", StoryTaskToolSpec.STORY_FLOW_GATE);
        payload.put("decision", decision);
        callResult.setPayload(payload);
        return callResult;
    }

    private ToolCallResult executeStorySceneGenerate(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryOneClickSceneGenerateDto dto = buildStorySceneRequest(context, request);
        Map<String, Object> payload = buildStoryPayload(context, request, "scene", StoryTaskToolSpec.STORY_GENERATE_SCENE);
        TsStoryOneClickSceneGenerateVo result = this.storyGenerateService.generateStoryScene(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("storySceneResult", result);
            context.putAttribute("storySceneResultJson", resultJson);
            context.putAttribute("storyBackgroundResult", result);
            context.putAttribute("storyBackgroundResultJson", resultJson);
            context.putAttribute("storyFlowStage", "done");
        }
        ToolCallResult callResult = ToolCallResult.success("已生成故事背景", result);
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
        dto.setExtraInfo(firstText(args, promptVariables, "extraInfo", "extra_info", "userInput", "user_input"));
        dto.normalize();
        return dto;
    }

    private TsStoryOneClickSceneGenerateDto buildStorySceneRequest(AgentContext context, ToolCallRequest toolRequest) {
        TsStoryOneClickSceneGenerateDto dto = new TsStoryOneClickSceneGenerateDto();
        Map<String, Object> args = toolRequest == null ? null : toolRequest.getArguments();
        Map<String, Object> promptVariables = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        dto.setTitle(firstText(args, promptVariables, "title"));
        dto.setStoryMode(firstText(args, promptVariables, "storyMode", "story_mode"));
        dto.setStorySetting(firstText(args, promptVariables, "storySetting", "story_setting"));
        dto.setStoryIntro(firstText(args, promptVariables, "storyIntro", "story_intro"));
        dto.setStoryBackground(firstText(args, promptVariables, "storyBackground", "story_background", "userInput", "user_input"));
        dto.setSceneSetting(firstText(args, promptVariables, "sceneSetting", "scene_setting"));
        dto.setPlotOutline(firstText(args, promptVariables, "plotOutline", "plot_outline"));
        dto.setStyleHint(firstText(args, promptVariables, "styleHint", "style_hint"));
        dto.setTemplateMode(firstText(args, promptVariables, "templateMode", "template_mode"));
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
        return payload;
    }

    private String normalizeConfirmationAction(String action) {
        if (!StringUtils.hasText(action)) {
            return "ASK_USER";
        }
        String value = action.trim().toUpperCase();
        if ("ACCEPT_AND_CONTINUE".equals(value)
                || "REGENERATE".equals(value)
                || "MODIFY".equals(value)
                || "ASK_USER".equals(value)) {
            return value;
        }
        return "ASK_USER";
    }

    private List<String> normalizeOptions(Object rawOptions) {
        List<String> options = new ArrayList<>();
        if (rawOptions instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    options.add(String.valueOf(item).trim());
                }
            }
        } else if (rawOptions instanceof String text && StringUtils.hasText(text)) {
            try {
                List<String> parsed = com.alibaba.fastjson.JSON.parseArray(text, String.class);
                if (parsed != null) {
                    for (String item : parsed) {
                        if (StringUtils.hasText(item)) {
                            options.add(item.trim());
                        }
                    }
                }
            } catch (Exception ignored) {
                options.add(text.trim());
            }
        }
        if (options.isEmpty()) {
            options.add("我觉得这个可以，继续生成故事背景");
            options.add("帮我重新生成一个");
        }
        return options;
    }

    private String buildConfirmationReply(String action) {
        if ("ACCEPT_AND_CONTINUE".equals(action)) {
            return "好的，我继续为这个故事生成背景和场景。";
        }
        if ("REGENERATE".equals(action)) {
            return "好的，我帮你重新生成一版故事。";
        }
        if ("MODIFY".equals(action)) {
            return "好的，我会按你的修改意见调整这版故事。";
        }
        return "这版故事你想继续完善，还是重新生成一版？";
    }

    private String buildConfirmationReason(String action) {
        if ("ACCEPT_AND_CONTINUE".equals(action)) {
            return "LLM判断用户接受当前故事并希望继续。";
        }
        if ("REGENERATE".equals(action)) {
            return "LLM判断用户希望重新生成故事。";
        }
        if ("MODIFY".equals(action)) {
            return "LLM判断用户希望局部修改当前故事。";
        }
        return "LLM判断用户意图不够明确，需要提供选择。";
    }

    private boolean hasStoryCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("storyCoreResultJson") != null
                || context.getAttribute("storyCorePresetResultJson") != null
                || context.getAttribute("storyFullGenerateResultJson") != null;
    }

    private String firstNonBlank(AgentContext context, ToolCallRequest request, String... keys) {
        String value = normalizeText(request, keys == null || keys.length == 0 ? null : keys[0]);
        if (StringUtils.hasText(value)) {
            return value;
        }
        Map<String, Object> promptVariables = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        value = firstText(promptVariables, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        if (context != null && StringUtils.hasText(context.getUserInput())) {
            return context.getUserInput().trim();
        }
        return null;
    }

    private String firstNonBlank(Map<String, Object> source, String fallback, String... keys) {
        String value = firstText(source, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }

    private String normalizeText(ToolCallRequest request, String key) {
        if (request == null || request.getArguments() == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = request.getArguments().get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstTextWithDefault(Map<String, Object> source, String defaultValue, String... keys) {
        String value = firstText(source, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return defaultValue;
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
