package org.jeecg.modules.system.agent.task.role;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.common.SubAgentHistorySupport;
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
    private static final String ROUTE_ROLE_FLOW_GATE = "ROLE_FLOW_GATE";
    private static final String ROUTE_ROLE_IMAGE_GENERATE = "ROLE_IMAGE_GENERATE";
    private static final String ROUTE_ROLE_VOICE_GENERATE = "ROLE_VOICE_GENERATE";

    private final ToolRegistry toolRegistry;
    private final ITsRoleGenerateService roleGenerateService;

    public RoleTaskToolRegistrar(ToolRegistry toolRegistry,
                                 ITsRoleGenerateService roleGenerateService) {
        this.toolRegistry = toolRegistry;
        this.roleGenerateService = roleGenerateService;
    }

    @PostConstruct
    public void registerTools() {
        this.toolRegistry.register(buildRoleCoreFillPresetTool());
        this.toolRegistry.register(buildRoleGenerateRoleTool());
        this.toolRegistry.register(buildRoleFlowGateTool());
        this.toolRegistry.register(buildRoleImageGenerateTool());
        this.toolRegistry.register(buildRoleVoiceGenerateTool());
    }

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

    private ToolDefinition buildRoleFlowGateTool() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_FLOW_GATE);
        definition.setRouteKey(ROUTE_ROLE_FLOW_GATE);
        definition.setCategory("role_task");
        definition.setDisplayName("角色流程门禁");
        definition.setDescription("判断角色核心结果是否可以进入下一阶段");
        definition.setExecutor(this::executeRoleFlowGate);
        return definition;
    }

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
        Map<String, Object> payload = buildRolePayload(context, request, "preset", RoleTaskToolSpec.ROLE_CORE_FILL_PRESET);
        Object result = this.roleGenerateService.generateRoleSettingPreset(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("roleCorePresetResult", result);
            context.putAttribute("roleCorePresetResultJson", resultJson);
            context.putAttribute("roleCoreResult", result);
            context.putAttribute("roleCoreResultJson", resultJson);
            context.putAttribute("roleFlowStage", "core_confirm");
        }
        ToolCallResult callResult = ToolCallResult.success("已生成角色核心设定", result);
        callResult.setPayload(payload);
        return callResult;
    }

    private ToolCallResult executeRoleGenerateRole(AgentContext context, ToolCallRequest request) {
        LoginUser user = TaskAgentSupport.buildLoginUser(context);
        String userInput = firstNonBlank(context, request, "userInput", "user_input");
        TsRoleGenerateRoleDto dto = new TsRoleGenerateRoleDto();
        dto.setStorySetting(firstNonBlank(context, request, "storySetting", "story_setting"));
        dto.setStoryBackground(firstNonBlank(context, request, "storyBackground", "story_background", "userInput", "user_input"));
        if (!StringUtils.hasText(dto.getStorySetting()) && StringUtils.hasText(userInput)) {
            dto.setStorySetting(userInput);
        }
        if (!StringUtils.hasText(dto.getStoryBackground()) && StringUtils.hasText(userInput)) {
            dto.setStoryBackground(userInput);
        }
        dto.normalize();
        Map<String, Object> payload = buildRolePayload(context, request, "full", RoleTaskToolSpec.ROLE_GENERATE_ROLE);
        TsRoleGenerateRoleVo result = this.roleGenerateService.generateRole(user, dto);
        payload.put("result", result);
        payload.put("resultJson", JSONObject.toJSONString(result));
        if (context != null) {
            String resultJson = JSONObject.toJSONString(result);
            context.putAttribute("roleGenerateRoleResult", result);
            context.putAttribute("roleGenerateRoleResultJson", resultJson);
            if (result != null && result.getSettingResult() != null) {
                String coreJson = JSONObject.toJSONString(result.getSettingResult());
                context.putAttribute("roleCoreResult", result.getSettingResult());
                context.putAttribute("roleCoreResultJson", coreJson);
            }
            context.putAttribute("roleFlowStage", "core_confirm");
        }
        ToolCallResult callResult = ToolCallResult.success("已生成完整角色", result);
        callResult.setPayload(payload);
        return callResult;
    }

    private ToolCallResult executeRoleFlowGate(AgentContext context, ToolCallRequest request) {
        String stage = firstNonBlank(context, request, "stage");
        if (!StringUtils.hasText(stage) && context != null) {
            stage = oConvertUtils.getString(context.getAttribute("roleFlowStage"));
        }
        if (!StringUtils.hasText(stage) && hasRoleCoreState(context)) {
            stage = "core_confirm";
        }
        boolean shouldWait = "core_confirm".equalsIgnoreCase(stage)
                || "core".equalsIgnoreCase(stage)
                || "preset".equalsIgnoreCase(stage);
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("action", shouldWait ? "WAIT_CONFIRM" : "NEXT");
        decision.put("nextStage", shouldWait ? "image" : "done");
        decision.put("question", shouldWait ? "你对这版角色满意吗？想先改哪部分？" : null);
        decision.put("reason", shouldWait ? "核心设定已生成，先确认再继续补形象与声音" : "当前阶段可继续");
        if (context != null) {
            context.putAttribute("roleFlowGateDecision", decision);
            context.putAttribute("roleFlowGateDecisionJson", JSONObject.toJSONString(decision));
            context.putAttribute("roleFlowStage", shouldWait ? "core_confirm" : "done");
        }
        ToolCallResult callResult = ToolCallResult.success(shouldWait ? "需要用户确认" : "可以继续下一步", decision);
        Map<String, Object> payload = buildCommonPayload(context, request, "gate", RoleTaskToolSpec.ROLE_FLOW_GATE);
        payload.put("decision", decision);
        callResult.setPayload(payload);
        return callResult;
    }

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
            context.putAttribute("roleImageResult", result);
            context.putAttribute("roleImageResultJson", resultJson);
        }
        callResult.setPayload(payload);
        return callResult;
    }

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
            context.putAttribute("roleVoiceResult", result);
            context.putAttribute("roleVoiceResultJson", resultJson);
        }
        callResult.setPayload(payload);
        return callResult;
    }

    private Map<String, Object> buildRolePayload(AgentContext context, ToolCallRequest request, String executionMode, String toolName) {
        Map<String, Object> payload = buildCommonPayload(context, request, executionMode, toolName);
        payload.put("stage", "role_generate");
        return payload;
    }

    private Map<String, Object> buildCommonPayload(AgentContext context, ToolCallRequest request, String executionMode, String toolName) {
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

    private boolean hasRoleCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("roleCoreResultJson") != null
                || context.getAttribute("roleCorePresetResultJson") != null
                || context.getAttribute("roleGenerateRoleResultJson") != null;
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

    private String firstText(Map<String, Object> firstSource, Map<String, Object> secondSource, String... keys) {
        String value = firstText(firstSource, keys);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return firstText(secondSource, keys);
    }

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
