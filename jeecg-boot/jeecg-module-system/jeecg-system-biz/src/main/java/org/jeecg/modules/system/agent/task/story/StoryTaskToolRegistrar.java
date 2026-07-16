package org.jeecg.modules.system.agent.task.story;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.jeecg.common.system.vo.LoginUser;
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
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneGenerateVo;
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
    private static final String ROUTE_STORY_SCENE_GENERATE = "STORY_SCENE_GENERATE";

    private final ToolRegistry toolRegistry;
    private final ITsStoryGenerateService storyGenerateService;

    /**
     * 注入工具注册中心和故事生成业务服务。
     */
    public StoryTaskToolRegistrar(ToolRegistry toolRegistry,
                                  ITsStoryGenerateService storyGenerateService) {
        this.toolRegistry = toolRegistry;
        this.storyGenerateService = storyGenerateService;
    }

    /**
     * 容器启动后注册故事生成相关的三个业务工具。
     */
    @PostConstruct
    public void registerTools() {
        this.toolRegistry.register(buildStoryFullGenerateTool());
        this.toolRegistry.register(buildStoryFullGeneratePresetTool());
        this.toolRegistry.register(buildStorySceneGenerateTool());
    }

    /**
     * 构建完整故事生成工具定义。
     */
    private ToolDefinition buildStoryFullGenerateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(StoryTaskToolSpec.STORY_FULL_GENERATE);
        definition.setRouteKey(ROUTE_STORY_FULL_GENERATE);
        definition.setCategory("story_task");
        definition.setDisplayName("完整故事生成");
        definition.setDescription("适合故事信息较完整、直接生成完整故事时使用；可通过extraInfo补充不属于核心字段的故事信息");
        definition.setExecutor(this::executeStoryFullGenerate);
        return definition;
    }

    /**
     * 构建故事核心字段补全工具定义。
     */
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

    /**
     * 构建故事背景和场景生成工具定义。
     */
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

    /**
     * 根据用户输入和故事字段生成完整故事，并保存故事核心 JSON。
     */
    private ToolCallResult executeStoryFullGenerate(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryFullGenerateDto dto = buildStoryRequest(context, request);
        Map<String, Object> payload = buildCommonPayload(context, request, "full", StoryTaskToolSpec.STORY_FULL_GENERATE);
        TsStoryFullGenerateVo result = this.storyGenerateService.generateStoryFull(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("storyFullGenerateResultJson", resultJson);
            context.putAttribute("storyCoreResultJson", resultJson);
        }
        ToolCallResult callResult = ToolCallResult.success("已生成完整故事", result);
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 根据少量输入补全故事核心字段，并保存故事核心 JSON。
     */
    private ToolCallResult executeStoryFullGeneratePreset(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryFullGenerateDto dto = buildStoryRequest(context, request);
        Map<String, Object> payload = buildCommonPayload(context, request, "preset", StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET);
        TsStoryFullGenerateVo result = this.storyGenerateService.generateStoryFullPreset(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("storyCorePresetResultJson", resultJson);
            context.putAttribute("storyCoreResultJson", resultJson);
        }
        ToolCallResult callResult = ToolCallResult.success("已生成故事 preset", result);
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 根据已确认的故事核心生成背景和场景，并保存结果 JSON。
     */
    private ToolCallResult executeStorySceneGenerate(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsStoryOneClickSceneGenerateDto dto = buildStorySceneRequest(context, request);
        Map<String, Object> payload = buildCommonPayload(context, request, "scene", StoryTaskToolSpec.STORY_GENERATE_SCENE);
        TsStoryOneClickSceneGenerateVo result = this.storyGenerateService.generateStoryScene(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("storySceneResultJson", resultJson);
            context.putAttribute("storyBackgroundResultJson", resultJson);
        }
        ToolCallResult callResult = ToolCallResult.success("已生成故事背景", result);
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 从工具参数和提示变量构建完整故事生成请求。
     */
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

    /**
     * 从工具参数和提示变量构建故事场景生成请求。
     */
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

    /**
     * 构建工具通用返回数据，包括工具名称、执行模式、参数和历史数量。
     */
    private Map<String, Object> buildCommonPayload(AgentContext context, ToolCallRequest request, String executionMode, String toolName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolName", toolName);
        payload.put("executionMode", executionMode);
        if (request != null) {
            payload.put("arguments", request.getArguments());
        }
        return payload;
    }

    /**
     * 依次从两个数据源中读取首个非空文本。
     */
    private String firstText(Map<String, Object> firstSource, Map<String, Object> secondSource, String... keys) {
        String value = firstText(firstSource, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return firstText(secondSource, keys);
    }

    /**
     * 按字段别名顺序从数据源中读取首个非空文本。
     */
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
