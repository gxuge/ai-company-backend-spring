package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateVoiceNode;
import org.jeecg.modules.airag.agent.subagent.role.tool.RoleContinueGenerationToolContract;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色子 Agent。
 *
 * <p>角色对话节点通过 Tool 展示确认，用户明确同意后由继续生成 Tool 进入后续阶段。</p>
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
    private final RoleCreateImageNode roleCreateImageNode;
    private final RoleCreateVoiceNode roleCreateVoiceNode;

    public RoleTaskSubAgent(NodeRunner nodeRunner,
                            RoleCreateDialogNode roleCreateDialogNode,
                            RoleCreateImageNode roleCreateImageNode,
                            RoleCreateVoiceNode roleCreateVoiceNode) {
        this.nodeRunner = nodeRunner;
        this.roleCreateDialogNode = roleCreateDialogNode;
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
            Map<String, Object> pendingInteraction = UserInteractionSupport.getPending(context);
            if (!pendingInteraction.isEmpty()) {
                AgentResult pendingResult = handlePendingInteraction(context, chainData, pendingInteraction);
                if (pendingResult != null) {
                    return pendingResult;
                }
            }

            String stage = resolveStage(context);
            if (STAGE_VOICE.equals(stage)) {
                return continueWithVoice(context, chainData, null);
            }
            if (STAGE_IMAGE.equals(stage)) {
                return continueWithImageAndVoice(context, chainData);
            }
            return continueWithDialog(context, chainData);
        } catch (Exception ex) {
            AgentResult result = AgentResult.failed(ex.getMessage());
            result.getData().putAll(chainData);
            result.getData().put("stage", "failed");
            AgentFlowStateSupport.attachResumeData(result, context);
            return result;
        }
    }

    /**
     * 处理 Tool 创建的展示交互；用户回复后只清理交互并重新进入角色对话。
     */
    private AgentResult handlePendingInteraction(AgentContext context,
                                                 Map<String, Object> chainData,
                                                 Map<String, Object> pendingInteraction) {
        if (context == null || !oConvertUtils.isNotEmpty(context.getUserInput())) {
            return waitingInteraction(context, chainData, pendingInteraction);
        }
        UserInteractionSupport.clear(context);
        return null;
    }

    /**
     * 继续角色对话节点；只有节点显式调用确认 Tool 后才进入确认等待态。
     */
    private AgentResult continueWithDialog(AgentContext context, Map<String, Object> chainData) {
        markStage(context, STAGE_DIALOG, this.roleCreateDialogNode.nodeName());
        NodeResult dialogResult = this.nodeRunner.run(context, this.roleCreateDialogNode);
        storeNodeResult(context, "roleDialogNodeResult", dialogResult);
        AgentResult dialogHandoff = handoffIfNeeded(context, dialogResult, chainData, STAGE_DIALOG);
        if (dialogHandoff != null) {
            return dialogHandoff;
        }
        if (RoleContinueGenerationToolContract.consumeContinueRequested(context)) {
            return continueWithImageAndVoice(context, chainData);
        }

        Map<String, Object> pendingInteraction = UserInteractionSupport.getPending(context);
        if (!pendingInteraction.isEmpty()) {
            return waitingInteraction(context, chainData, pendingInteraction);
        }
        return waiting(
                context,
                dialogResult == null ? null : dialogResult.getContent(),
                chainData,
                STAGE_DIALOG
        );
    }

    private AgentResult continueWithImageAndVoice(AgentContext context, Map<String, Object> chainData) {
        markStage(context, STAGE_IMAGE, this.roleCreateImageNode.nodeName());
        NodeResult imageResult = this.nodeRunner.run(context, this.roleCreateImageNode);
        storeNodeResult(context, "roleImageNodeResult", imageResult);
        AgentResult imageHandoff = handoffIfNeeded(context, imageResult, chainData, STAGE_IMAGE);
        if (imageHandoff != null) {
            return imageHandoff;
        }

        return continueWithVoice(context, chainData, imageResult);
    }

    private AgentResult continueWithVoice(AgentContext context,
                                          Map<String, Object> chainData,
                                          NodeResult imageResult) {
        markStage(context, STAGE_VOICE, this.roleCreateVoiceNode.nodeName());
        NodeResult voiceResult = this.nodeRunner.run(context, this.roleCreateVoiceNode);
        storeNodeResult(context, "roleVoiceNodeResult", voiceResult);
        AgentResult voiceHandoff = handoffIfNeeded(context, voiceResult, chainData, STAGE_VOICE);
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

    /**
     * 返回 Tool 创建的结构化确认交互。
     */
    private AgentResult waitingInteraction(AgentContext context,
                                           Map<String, Object> chainData,
                                           Map<String, Object> interaction) {
        String question = oConvertUtils.getString(interaction.get("question"));
        if (!oConvertUtils.isNotEmpty(question)) {
            question = "你对这版角色满意吗？";
        }
        markStage(context, STAGE_CONFIRMATION, this.roleCreateDialogNode.nodeName());
        AgentResult result = AgentResult.waitingUser(question);
        result.setStructuredResult(interaction);
        result.getData().putAll(chainData);
        result.getData().put("stage", STAGE_CONFIRMATION);
        result.getData().put("question", question);
        result.getData().put("status", "WAITING_USER");
        copyInteractionField(result, interaction, "interactionId");
        copyInteractionField(result, interaction, "interactionType");
        copyInteractionField(result, interaction, "options");
        copyInteractionField(result, interaction, "suspendRun");
        AgentFlowStateSupport.attachResumeData(result, context);
        return result;
    }

    private AgentResult waiting(AgentContext context,
                                String content,
                                Map<String, Object> chainData,
                                String stage) {
        String text = oConvertUtils.isNotEmpty(content) ? content : "请继续补充角色设定。";
        markStage(context, stage, this.roleCreateDialogNode.nodeName());
        AgentResult result = AgentResult.waitingUser(text);
        result.setStructuredResult(chainData);
        result.getData().putAll(chainData);
        result.getData().put("stage", stage);
        result.getData().put("question", text);
        result.getData().put("status", "WAITING_USER");
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
        }
        if (!oConvertUtils.isNotEmpty(stage) || STAGE_CONFIRMATION.equalsIgnoreCase(stage)) {
            return STAGE_DIALOG;
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

    private AgentResult handoffIfNeeded(AgentContext context,
                                        Object nodeResult,
                                        Map<String, Object> chainData,
                                        String stage) {
        boolean shouldHandoff = nodeResult instanceof NodeResult result
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
        if (nodeResult instanceof NodeResult result) {
            context.putAttribute(key, result.getData());
            context.putAttribute(key + "Content", result.getContent());
            context.putAttribute(key + "Json", com.alibaba.fastjson2.JSON.toJSONString(result.getData()));
            return;
        }
        context.putAttribute(key, nodeResult);
    }

    private void copyInteractionField(AgentResult result,
                                      Map<String, Object> interaction,
                                      String fieldName) {
        Object value = interaction.get(fieldName);
        if (value != null) {
            result.getData().put(fieldName, value);
        }
    }

    private Map<String, Object> buildStructuredResult(AgentContext context,
                                                      Object imageResult,
                                                      Object voiceResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleCoreResultJson", context == null ? null : context.getAttribute("roleCoreResultJson"));
        result.put(
                RoleContinueGenerationToolContract.TRANSFER_DATA_JSON,
                context == null ? null : context.getAttribute(RoleContinueGenerationToolContract.TRANSFER_DATA_JSON)
        );
        result.put("roleImageResultJson", context == null ? null : context.getAttribute("roleImageResultJson"));
        result.put("roleVoiceResultJson", context == null ? null : context.getAttribute("roleVoiceResultJson"));
        if (imageResult instanceof NodeResult imageNodeResult) {
            result.put("imageResult", imageNodeResult.getData());
            result.put("imageContent", imageNodeResult.getContent());
        } else {
            result.put("imageResult", imageResult);
        }
        if (voiceResult instanceof NodeResult voiceNodeResult) {
            result.put("voiceResult", voiceNodeResult.getData());
            result.put("voiceContent", voiceNodeResult.getContent());
        } else {
            result.put("voiceResult", voiceResult);
        }
        return result;
    }
}
