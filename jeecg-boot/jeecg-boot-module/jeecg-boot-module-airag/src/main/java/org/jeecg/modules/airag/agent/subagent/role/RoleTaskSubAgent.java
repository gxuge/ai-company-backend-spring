package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleConfirmationNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateVoiceNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 角色子 Agent。
 *
 * <p>执行顺序：角色对话 -> 用户确认 -> 形象 -> 声音。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
@Component
public class RoleTaskSubAgent implements SubAgent {
    private static final String STAGE_DIALOG = "dialog";
    private static final String STAGE_CONFIRMATION = "confirmation";
    private static final String STAGE_IMAGE = "image";
    private static final String STAGE_VOICE = "voice";

    private final NodeRunner nodeRunner;
    private final RoleCreateDialogNode roleCreateDialogNode;
    private final RoleConfirmationNode roleConfirmationNode;
    private final RoleCreateImageNode roleCreateImageNode;
    private final RoleCreateVoiceNode roleCreateVoiceNode;

    public RoleTaskSubAgent(NodeRunner nodeRunner,
                            RoleCreateDialogNode roleCreateDialogNode,
                            RoleConfirmationNode roleConfirmationNode,
                            RoleCreateImageNode roleCreateImageNode,
                            RoleCreateVoiceNode roleCreateVoiceNode) {
        this.nodeRunner = nodeRunner;
        this.roleCreateDialogNode = roleCreateDialogNode;
        this.roleConfirmationNode = roleConfirmationNode;
        this.roleCreateImageNode = roleCreateImageNode;
        this.roleCreateVoiceNode = roleCreateVoiceNode;
    }

    @Override
    public String subAgentName() {
        return RoleTaskChainSpec.SUB_AGENT_NAME;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, Object> chainData = buildChainData();
        if (context != null) {
            context.putAttribute("roleTaskChainSpec", chainData);
        }

        try {
            String stage = resolveStage(context);
            boolean continueExistingDialog = (STAGE_CONFIRMATION.equals(stage) || STAGE_DIALOG.equals(stage))
                    && hasRoleCoreState(context)
                    && !this.roleConfirmationNode.hasOptionValue(context);
            String roleCoreStateBeforeDialog = continueExistingDialog
                    ? roleCoreStateSnapshot(context)
                    : null;
            if (STAGE_VOICE.equals(stage)) {
                return continueWithVoice(context, chainData, null);
            }
            if (STAGE_IMAGE.equals(stage)) {
                return continueWithImageAndVoice(context, chainData);
            }
            if ((STAGE_CONFIRMATION.equals(stage) || hasRoleCoreState(context)) && hasRoleCoreState(context)) {
                if (this.roleConfirmationNode.hasOptionValue(context)) {
                    AgentResult decisionResult = handleConfirmationOption(context, chainData);
                    if (decisionResult != null) {
                        return decisionResult;
                    }
                }
            }

            markStage(context, STAGE_DIALOG, this.roleCreateDialogNode.nodeName());
            var dialogResult = this.nodeRunner.run(context, this.roleCreateDialogNode);
            storeNodeResult(context, "roleDialogNodeResult", dialogResult);
            AgentResult dialogHandoff = handoffIfNeeded(context, dialogResult, chainData, "dialog");
            if (dialogHandoff != null) {
                return dialogHandoff;
            }

            if (!hasRoleCoreState(context)) {
                return waiting(context, dialogResult == null ? null : dialogResult.getContent(), chainData, STAGE_DIALOG);
            }
            if (continueExistingDialog
                    && Objects.equals(roleCoreStateBeforeDialog, roleCoreStateSnapshot(context))) {
                return waiting(context, dialogResult == null ? null : dialogResult.getContent(), chainData, STAGE_DIALOG);
            }

            return handleConfirmationOption(context, chainData);
        } catch (Exception ex) {
            AgentResult result = AgentResult.failed(ex.getMessage());
            result.getData().putAll(chainData);
            result.getData().put("stage", "failed");
            AgentFlowStateSupport.attachResumeData(result, context);
            return result;
        }
    }

    private AgentResult handleConfirmationOption(AgentContext context, Map<String, Object> chainData) {
        markStage(context, STAGE_CONFIRMATION, this.roleConfirmationNode.nodeName());
        var confirmationResult = this.nodeRunner.run(context, this.roleConfirmationNode);
        storeNodeResult(context, "roleConfirmationNodeResult", confirmationResult);
        AgentResult handoff = handoffIfNeeded(context, confirmationResult, chainData, STAGE_CONFIRMATION);
        if (handoff != null) {
            return handoff;
        }
        Map<String, Object> decision = extractDecision(confirmationResult);
        if (context != null) {
            this.roleConfirmationNode.consumeOptionValue(context);
            context.putAttribute("roleConfirmationDecision", decision);
        }
        return applyConfirmationDecision(
                context,
                chainData,
                decision,
                confirmationResult == null ? null : confirmationResult.getContent()
        );
    }

    private AgentResult applyConfirmationDecision(AgentContext context,
                                                  Map<String, Object> chainData,
                                                  Map<String, Object> decision,
                                                  String fallbackContent) {
        String action = oConvertUtils.getString(decision == null ? null : decision.get("action"));
        if ("ACCEPT_AND_CONTINUE".equalsIgnoreCase(action)) {
            return continueWithImageAndVoice(context, chainData);
        }
        if ("REGENERATE".equalsIgnoreCase(action) || "MODIFY".equalsIgnoreCase(action)) {
            return null;
        }
        String content = oConvertUtils.getString(decision == null ? null : decision.get("question"));
        if (!oConvertUtils.isNotEmpty(content)) {
            content = oConvertUtils.getString(decision == null ? null : decision.get("reply"));
        }
        if (!oConvertUtils.isNotEmpty(content)) {
            content = fallbackContent;
        }
        return waiting(context, content, chainData, STAGE_CONFIRMATION, decision);
    }

    private AgentResult continueWithImageAndVoice(AgentContext context, Map<String, Object> chainData) {
        markStage(context, STAGE_IMAGE, this.roleCreateImageNode.nodeName());
        var imageResult = this.nodeRunner.run(context, this.roleCreateImageNode);
        storeNodeResult(context, "roleImageNodeResult", imageResult);
        AgentResult imageHandoff = handoffIfNeeded(context, imageResult, chainData, "image");
        if (imageHandoff != null) {
            return imageHandoff;
        }

        return continueWithVoice(context, chainData, imageResult);
    }

    private AgentResult continueWithVoice(AgentContext context,
                                          Map<String, Object> chainData,
                                          NodeResult imageResult) {
        markStage(context, STAGE_VOICE, this.roleCreateVoiceNode.nodeName());
        var voiceResult = this.nodeRunner.run(context, this.roleCreateVoiceNode);
        storeNodeResult(context, "roleVoiceNodeResult", voiceResult);
        AgentResult voiceHandoff = handoffIfNeeded(context, voiceResult, chainData, "voice");
        if (voiceHandoff != null) {
            return voiceHandoff;
        }

        String content = voiceResult == null ? null : voiceResult.getContent();
        if (!oConvertUtils.isNotEmpty(content) && imageResult != null) {
            content = imageResult.getContent();
        }
        if (!oConvertUtils.isNotEmpty(content) && context != null) {
            content = context.getLatestContent();
        }
        if (!oConvertUtils.isNotEmpty(content)) {
            content = "角色已生成完成";
        }
        if (context != null) {
            context.putAttribute("roleTaskStage", "done");
        }
        Object structuredResult = buildStructuredResult(context, imageResult, voiceResult);
        AgentResult result = AgentHandoffSupport.buildCompletedHandoffResult(
                context,
                subAgentName(),
                content,
                structuredResult
        );
        result.getData().putAll(chainData);
        result.getData().put("stage", "done");
        result.getData().put("image", imageResult == null ? null : imageResult.getData());
        result.getData().put("voice", voiceResult == null ? null : voiceResult.getData());
        return result;
    }

    private AgentResult waiting(AgentContext context, String content, Map<String, Object> chainData, String stage) {
        return waiting(context, content, chainData, stage, null);
    }

    private AgentResult waiting(AgentContext context,
                                String content,
                                Map<String, Object> chainData,
                                String stage,
                                Map<String, Object> decision) {
        String text = oConvertUtils.isNotEmpty(content) ? content : "你对这版角色满意吗？想先改哪部分？";
        String resumeNodeName = STAGE_CONFIRMATION.equals(stage)
                ? this.roleConfirmationNode.nodeName()
                : this.roleCreateDialogNode.nodeName();
        markStage(context, stage, resumeNodeName);
        AgentResult result = AgentResult.waitingUser(text);
        result.setStructuredResult(chainData);
        result.getData().putAll(chainData);
        result.getData().put("stage", stage);
        result.getData().put("question", text);
        result.getData().put("status", "WAITING_USER");
        if (decision != null && !decision.isEmpty()) {
            result.getData().put("decision", decision);
            Object options = decision.get("options");
            if (options != null) {
                result.getData().put("options", options);
            }
        }
        AgentFlowStateSupport.attachResumeData(result, context);
        return result;
    }

    private String resolveStage(AgentContext context) {
        if (context == null) {
            return STAGE_DIALOG;
        }
        String stage = oConvertUtils.getString(context.getActiveStage());
        if (!oConvertUtils.isNotEmpty(stage)) {
            stage = oConvertUtils.getString(context.getAttribute("roleTaskStage"));
        }
        if (!oConvertUtils.isNotEmpty(stage)) {
            String resumeNodeName = oConvertUtils.getString(context.getResumeNodeName());
            if (this.roleCreateVoiceNode.nodeName().equalsIgnoreCase(resumeNodeName)) {
                return STAGE_VOICE;
            }
            if (this.roleCreateImageNode.nodeName().equalsIgnoreCase(resumeNodeName)) {
                return STAGE_IMAGE;
            }
            if (this.roleConfirmationNode.nodeName().equalsIgnoreCase(resumeNodeName)) {
                return STAGE_CONFIRMATION;
            }
        }
        if (!oConvertUtils.isNotEmpty(stage)) {
            return hasRoleCoreState(context) ? STAGE_CONFIRMATION : STAGE_DIALOG;
        }
        return stage.trim().toLowerCase();
    }

    private void markStage(AgentContext context, String stage, String resumeNodeName) {
        if (context == null) {
            return;
        }
        context.putAttribute("roleTaskStage", stage);
        AgentFlowStateSupport.markResume(context, resumeNodeName, stage);
    }

    private AgentResult handoffIfNeeded(AgentContext context, Object nodeResult, Map<String, Object> chainData, String stage) {
        boolean shouldHandoff = nodeResult instanceof org.jeecg.modules.airag.agent.graph.NodeResult result
                && AgentHandoffSupport.isHandoff(result);
        if (!shouldHandoff && context != null) {
            shouldHandoff = !AgentHandoffSupport.getHandoffPayload(context).isEmpty();
        }
        if (!shouldHandoff) {
            return null;
        }
        AgentResult result = AgentHandoffSupport.buildHandoffResult(context, subAgentName(), stage);
        result.getData().putAll(chainData);
        result.getData().put("stage", stage);
        return result;
    }

    private Map<String, Object> buildChainData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subAgentName", subAgentName());
        data.put("skills", RoleTaskChainSpec.SKILLS);
        data.put("tools", RoleTaskChainSpec.TOOLS);
        data.put("chain", RoleTaskChainSpec.CHAIN);
        return data;
    }

    private void storeNodeResult(AgentContext context, String key, Object nodeResult) {
        if (context == null || key == null || key.isBlank() || nodeResult == null) {
            return;
        }
        if (nodeResult instanceof org.jeecg.modules.airag.agent.graph.NodeResult result) {
            context.putAttribute(key, result.getData());
            context.putAttribute(key + "Content", result.getContent());
            context.putAttribute(key + "Json", com.alibaba.fastjson2.JSON.toJSONString(result.getData()));
            return;
        }
        context.putAttribute(key, nodeResult);
    }

    private boolean hasRoleCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("roleCoreResultJson") != null
                || context.getAttribute("roleCorePresetResultJson") != null
                || context.getAttribute("roleGenerateRoleResultJson") != null;
    }

    private String roleCoreStateSnapshot(AgentContext context) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("roleCoreResultJson", context == null ? null : context.getAttribute("roleCoreResultJson"));
        state.put("roleCorePresetResultJson", context == null ? null : context.getAttribute("roleCorePresetResultJson"));
        state.put("roleGenerateRoleResultJson", context == null ? null : context.getAttribute("roleGenerateRoleResultJson"));
        return com.alibaba.fastjson2.JSON.toJSONString(state);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDecision(Object nodeResult) {
        if (!(nodeResult instanceof org.jeecg.modules.airag.agent.graph.NodeResult result)) {
            return new LinkedHashMap<>();
        }
        if (result.getData().get("action") != null) {
            return copyStringKeyMap(result.getData());
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> copyStringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> decision = new LinkedHashMap<>();
        if (rawMap == null) {
            return decision;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                decision.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return decision;
    }

    private Map<String, Object> buildStructuredResult(AgentContext context, Object imageResult, Object voiceResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleCoreResultJson", context == null ? null : context.getAttribute("roleCoreResultJson"));
        result.put("roleImageResultJson", context == null ? null : context.getAttribute("roleImageResultJson"));
        result.put("roleVoiceResultJson", context == null ? null : context.getAttribute("roleVoiceResultJson"));
        if (imageResult instanceof org.jeecg.modules.airag.agent.graph.NodeResult imageNodeResult) {
            result.put("imageResult", imageNodeResult.getData());
            result.put("imageContent", imageNodeResult.getContent());
        } else {
            result.put("imageResult", imageResult);
        }
        if (voiceResult instanceof org.jeecg.modules.airag.agent.graph.NodeResult voiceNodeResult) {
            result.put("voiceResult", voiceNodeResult.getData());
            result.put("voiceContent", voiceNodeResult.getContent());
        } else {
            result.put("voiceResult", voiceResult);
        }
        return result;
    }
}
