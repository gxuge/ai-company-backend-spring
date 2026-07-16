package org.jeecg.modules.system.agent.task.role;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleTaskToolSpec;
import org.jeecg.modules.airag.agent.task.TaskAgentSupport;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.service.ITsRoleGenerateService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色任务真实工具注册器。
 *
 * <p>放在 system-biz 侧作为业务适配器，负责把 Agent 工具调用转发到现有角色生成服务。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
@Component
public class RoleTaskToolRegistrar {

    private static final String ROUTE_ROLE_CORE_FILL = "ROLE_CORE_FILL";
    private static final String ROUTE_ROLE_FULL_GENERATE = "ROLE_FULL_GENERATE";
    private static final String ROUTE_ROLE_IMAGE_GENERATE = "ROLE_IMAGE_GENERATE";
    private static final String ROUTE_ROLE_VOICE_GENERATE = "ROLE_VOICE_GENERATE";

    private final ToolRegistry toolRegistry;
    private final ITsRoleGenerateService roleGenerateService;

    /**
     * 注入工具注册中心和角色生成业务服务。
     */
    public RoleTaskToolRegistrar(ToolRegistry toolRegistry,
                                 ITsRoleGenerateService roleGenerateService) {
        this.toolRegistry = toolRegistry;
        this.roleGenerateService = roleGenerateService;
    }

    /**
     * 容器启动后注册角色生成相关的四个业务工具。
     */
    @PostConstruct
    public void registerTools() {
        this.toolRegistry.register(buildRoleCoreFillPresetTool());
        this.toolRegistry.register(buildRoleGenerateRoleTool());
        this.toolRegistry.register(buildRoleImageGenerateTool());
        this.toolRegistry.register(buildRoleVoiceGenerateTool());
    }

    /**
     * 构建角色核心设定补全工具定义。
     */
    private ToolDefinition buildRoleCoreFillPresetTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_CORE_FILL_PRESET);
        definition.setRouteKey(ROUTE_ROLE_CORE_FILL);
        definition.setCategory("role_task");
        definition.setDisplayName("角色核心设定补全");
        definition.setDescription("适合角色信息较少、需要先补全核心设定时使用");
        definition.setExecutor(this::executeRoleCoreFillPreset);
        return definition;
    }

    /**
     * 构建完整角色生成工具定义。
     */
    private ToolDefinition buildRoleGenerateRoleTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_GENERATE_ROLE);
        definition.setRouteKey(ROUTE_ROLE_FULL_GENERATE);
        definition.setCategory("role_task");
        definition.setDisplayName("完整角色生成");
        definition.setDescription("适合角色信息较完整、直接生成完整角色设定时使用");
        definition.setExecutor(this::executeRoleGenerateRole);
        return definition;
    }

    /**
     * 构建角色形象生成工具定义。
     */
    private ToolDefinition buildRoleImageGenerateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_GENERATE_ROLE_IMAGE);
        definition.setRouteKey(ROUTE_ROLE_IMAGE_GENERATE);
        definition.setCategory("role_task");
        definition.setDisplayName("角色形象生成");
        definition.setDescription("基于角色核心设定生成角色形象与生图提示");
        definition.setExecutor(this::executeRoleGenerateRoleImage);
        return definition;
    }

    /**
     * 构建角色声音生成工具定义。
     */
    private ToolDefinition buildRoleVoiceGenerateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_GENERATE_ROLE_VOICE);
        definition.setRouteKey(ROUTE_ROLE_VOICE_GENERATE);
        definition.setCategory("role_task");
        definition.setDisplayName("角色声音生成");
        definition.setDescription("基于角色核心设定生成声音建议与音色推荐");
        definition.setExecutor(this::executeRoleGenerateRoleVoice);
        return definition;
    }

    /**
     * 根据用户输入和提示变量补全角色核心设定，并将结果写入运行上下文。
     */
    private ToolCallResult executeRoleCoreFillPreset(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        String userInput = firstNonBlank(context, request, "userInput", "user_input");
        Map<String, Object> promptVariables = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        TsRoleOneClickSettingGenerateDto dto = new TsRoleOneClickSettingGenerateDto();
        dto.setRoleName(firstText(promptVariables, "roleName", "role_name"));
        dto.setGender(firstText(promptVariables, "gender"));
        dto.setOccupation(firstText(promptVariables, "occupation"));
        dto.setBackgroundStory(firstNonBlank(promptVariables, userInput, "backgroundStory", "background_story"));
        dto.setGreeting(firstText(promptVariables, "greeting"));
        dto.setStyleHint(firstNonBlank(promptVariables, userInput, "styleHint", "style_hint"));
        dto.setKeywords(firstNonBlank(promptVariables, userInput, "keywords"));
        dto.normalize();
        Map<String, Object> payload = buildCommonPayload(context, request, "preset", RoleTaskToolSpec.ROLE_CORE_FILL_PRESET);
        Object result = this.roleGenerateService.generateRoleSettingPreset(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("roleCorePresetResultJson", resultJson);
            context.putAttribute("roleCoreResultJson", resultJson);
        }
        ToolCallResult callResult = ToolCallResult.success("已生成角色核心设定", result);
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 根据故事设定和背景生成完整角色，并保存完整结果及角色核心设定。
     */
    private ToolCallResult executeRoleGenerateRole(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsRoleGenerateRoleDto dto = new TsRoleGenerateRoleDto();
        dto.setStorySetting(firstNonBlank(context, request, "storySetting", "story_setting", "userInput", "user_input"));
        dto.setStoryBackground(firstNonBlank(context, request, "storyBackground", "story_background", "userInput", "user_input"));
        dto.normalize();
        Map<String, Object> payload = buildCommonPayload(context, request, "full", RoleTaskToolSpec.ROLE_GENERATE_ROLE);
        TsRoleGenerateRoleVo result = this.roleGenerateService.generateRole(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("roleGenerateRoleResultJson", resultJson);
            if (result != null && result.getSettingResult() != null) {
                String coreJson = JSONObject.toJSONString(result.getSettingResult());
                context.putAttribute("roleCoreResultJson", coreJson);
            }
        }
        ToolCallResult callResult = ToolCallResult.success("已生成完整角色", result);
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 根据角色信息生成角色形象，并将生成结果写入运行上下文。
     */
    private ToolCallResult executeRoleGenerateRoleImage(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsRoleOneClickImageGenerateDto dto = new TsRoleOneClickImageGenerateDto();
        Map<String, Object> args = request == null ? null : request.getArguments();
        Map<String, Object> promptVariables = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        dto.setRoleId(firstLong(args, promptVariables, "roleId", "role_id"));
        dto.setRoleName(firstText(args, promptVariables, "roleName", "role_name"));
        dto.setGender(firstText(args, promptVariables, "gender"));
        dto.setOccupation(firstText(args, promptVariables, "occupation"));
        dto.setBackgroundStory(firstText(args, promptVariables, "backgroundStory", "background_story"));
        dto.setStyleName(firstText(args, promptVariables, "styleName", "style_name"));
        dto.setAspectRatio(firstText(args, promptVariables, "aspectRatio", "aspect_ratio"));
        dto.setReferenceImageUrl(firstText(args, promptVariables, "referenceImageUrl", "reference_image_url"));
        dto.setAsyncGenerate(Boolean.FALSE);
        dto.normalize();
        TsRoleOneClickImageGenerateVo result = this.roleGenerateService.generateRoleImage(user, dto);
        ToolCallResult callResult = ToolCallResult.success("已生成角色形象", result);
        Map<String, Object> payload = buildCommonPayload(context, request, "role_image", RoleTaskToolSpec.ROLE_GENERATE_ROLE_IMAGE);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("roleImageResultJson", resultJson);
        }
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 根据角色信息生成声音建议和音色结果，并写入运行上下文。
     */
    private ToolCallResult executeRoleGenerateRoleVoice(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        TsRoleOneClickVoiceGenerateDto dto = new TsRoleOneClickVoiceGenerateDto();
        Map<String, Object> args = request == null ? null : request.getArguments();
        Map<String, Object> promptVariables = TaskAgentSupport.readMapAttribute(context, "promptVariables");
        dto.setRoleId(firstLong(args, promptVariables, "roleId", "role_id"));
        dto.setRoleName(firstText(args, promptVariables, "roleName", "role_name"));
        dto.setGender(firstText(args, promptVariables, "gender"));
        dto.setOccupation(firstText(args, promptVariables, "occupation"));
        dto.setBackgroundStory(firstText(args, promptVariables, "backgroundStory", "background_story"));
        dto.setPreferredVoiceName(firstText(args, promptVariables, "preferredVoiceName", "preferred_voice_name"));
        dto.setTargetTone(firstText(args, promptVariables, "targetTone", "target_tone"));
        dto.setPreviewText(firstText(args, promptVariables, "previewText", "preview_text"));
        dto.normalize();
        TsRoleOneClickVoiceGenerateVo result = this.roleGenerateService.generateRoleVoice(user, dto);
        ToolCallResult callResult = ToolCallResult.success("已生成角色声音", result);
        Map<String, Object> payload = buildCommonPayload(context, request, "role_voice", RoleTaskToolSpec.ROLE_GENERATE_ROLE_VOICE);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("roleVoiceResultJson", resultJson);
        }
        callResult.setPayload(payload);
        return callResult;
    }

    /**
     * 构建工具通用返回数据，包括工具名称、执行模式、调用参数和历史数量。
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
     * 按工具参数、提示变量、用户原始输入的优先级读取首个非空文本。
     */
    private String firstNonBlank(AgentContext context, ToolCallRequest request, String... keys) {
        Map<String, Object> arguments = request == null ? null : request.getArguments();
        String value = firstText(arguments, keys);
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

    /**
     * 从指定数据源读取首个非空文本，未取到时使用兜底值。
     */
    private String firstNonBlank(Map<String, Object> source, String fallback, String... keys) {
        String value = firstText(source, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
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
     * 依次从两个数据源读取首个可转换为 Long 的值。
     */
    private Long firstLong(Map<String, Object> firstSource, Map<String, Object> secondSource, String... keys) {
        String value = firstText(firstSource, keys);
        if (StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value.trim());
            } catch (Exception ignored) {
                // ignore invalid number
            }
        }
        value = firstText(secondSource, keys);
        if (StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value.trim());
            } catch (Exception ignored) {
                // ignore invalid number
            }
        }
        return null;
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
