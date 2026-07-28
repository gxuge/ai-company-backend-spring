package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryContinueGenerationToolContract;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事子 Agent。
 *
 * <p>故事对话节点通过 Tool 展示确认，用户明确同意后由继续生成 Tool 进入背景 / 场景阶段。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryTaskSubAgent implements SubAgent {
    private static final String STAGE_DIALOG = "dialog";
    private static final String STAGE_CONFIRMATION = "confirmation";
    private static final String STAGE_BACKGROUND = "background";

    private final NodeRunner nodeRunner;
    private final StoryCreateDialogNode storyCreateDialogNode;
    private final StoryCreateBackgroundNode storyCreateBackgroundNode;

    public StoryTaskSubAgent(NodeRunner nodeRunner,
                             StoryCreateDialogNode storyCreateDialogNode,
                             StoryCreateBackgroundNode storyCreateBackgroundNode) {
        this.nodeRunner = nodeRunner;
        this.storyCreateDialogNode = storyCreateDialogNode;
        this.storyCreateBackgroundNode = storyCreateBackgroundNode;
    }

    @Override
    public String subAgentName() {
        return StoryTaskChainSpec.SUB_AGENT_NAME;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, Object> chainData = buildChainData();
        if (context != null) {
            context.putAttribute("storyTaskChainSpec", chainData);
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
            if (STAGE_BACKGROUND.equals(stage)) {
                return continueWithBackground(context, chainData);
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
     * 处理 Tool 创建的展示交互；用户回复后只清理交互并重新进入故事对话。
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
     * 继续故事对话节点；只有节点显式调用确认 Tool 后才进入确认等待态。
     */
    private AgentResult continueWithDialog(AgentContext context, Map<String, Object> chainData) {
        markStage(context, STAGE_DIALOG, this.storyCreateDialogNode.nodeName());
        NodeResult dialogResult = this.nodeRunner.run(context, this.storyCreateDialogNode);
        storeNodeResult(context, "storyDialogNodeResult", dialogResult);
        AgentResult dialogHandoff = handoffIfNeeded(context, dialogResult, chainData, STAGE_DIALOG);
        if (dialogHandoff != null) {
            return dialogHandoff;
        }
        if (StoryContinueGenerationToolContract.consumeContinueRequested(context)) {
            return continueWithBackground(context, chainData);
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

    private AgentResult continueWithBackground(AgentContext context, Map<String, Object> chainData) {
        markStage(context, STAGE_BACKGROUND, this.storyCreateBackgroundNode.nodeName());
        NodeResult backgroundResult = this.nodeRunner.run(context, this.storyCreateBackgroundNode);
        storeNodeResult(context, "storyBackgroundNodeResult", backgroundResult);
        AgentResult backgroundHandoff = handoffIfNeeded(context, backgroundResult, chainData, "background");
        if (backgroundHandoff != null) {
            return backgroundHandoff;
        }

        String content = backgroundResult == null ? null : backgroundResult.getContent();
        if (!oConvertUtils.isNotEmpty(content) && context != null) {
            content = context.getLatestContent();
        }
        if (!oConvertUtils.isNotEmpty(content)) {
            content = "故事已生成完成";
        }
        if (context != null) {
            context.putAttribute("storyTaskStage", "done");
        }
        Object structuredResult = buildStructuredResult(context, backgroundResult);
        AgentResult result = AgentHandoffSupport.buildCompletedHandoffResult(
                context,
                subAgentName(),
                content,
                structuredResult
        );
        result.getData().putAll(chainData);
        result.getData().put("stage", "done");
        result.getData().put("background", backgroundResult == null ? null : backgroundResult.getData());
        return result;
    }

    private AgentResult waiting(AgentContext context, String content, Map<String, Object> chainData, String stage) {
        String text = oConvertUtils.isNotEmpty(content) ? content : "请继续补充故事设定。";
        markStage(context, stage, this.storyCreateDialogNode.nodeName());
        AgentResult result = AgentResult.waitingUser(text);
        result.setStructuredResult(chainData);
        result.getData().putAll(chainData);
        result.getData().put("stage", stage);
        result.getData().put("question", text);
        result.getData().put("status", "WAITING_USER");
        AgentFlowStateSupport.attachResumeData(result, context);
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
            question = "你对这版故事满意吗？";
        }
        markStage(context, STAGE_CONFIRMATION, this.storyCreateDialogNode.nodeName());
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

    private String resolveStage(AgentContext context) {
        if (context == null) {
            return STAGE_DIALOG;
        }
        String stage = oConvertUtils.getString(context.getActiveStage());
        if (!oConvertUtils.isNotEmpty(stage)) {
            stage = oConvertUtils.getString(context.getAttribute("storyTaskStage"));
        }
        if (!oConvertUtils.isNotEmpty(stage)
                && this.storyCreateBackgroundNode.nodeName().equalsIgnoreCase(
                oConvertUtils.getString(context.getResumeNodeName()))) {
            return STAGE_BACKGROUND;
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
        context.putAttribute("storyTaskStage", stage);
        AgentFlowStateSupport.markResume(context, resumeNodeName, stage);
    }

    private AgentResult handoffIfNeeded(AgentContext context, Object nodeResult, Map<String, Object> chainData, String stage) {
        boolean shouldHandoff = nodeResult instanceof NodeResult result && AgentHandoffSupport.isHandoff(result);
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
        data.put("skills", StoryTaskChainSpec.SKILLS);
        data.put("tools", StoryTaskChainSpec.TOOLS);
        data.put("chain", StoryTaskChainSpec.CHAIN);
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

    private Map<String, Object> buildStructuredResult(AgentContext context, Object backgroundResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storyCoreResultJson", context == null ? null : context.getAttribute("storyCoreResultJson"));
        result.put(
                StoryContinueGenerationToolContract.TRANSFER_DATA_JSON,
                context == null ? null : context.getAttribute(StoryContinueGenerationToolContract.TRANSFER_DATA_JSON)
        );
        result.put("storyBackgroundResultJson", context == null ? null : context.getAttribute("storyBackgroundResultJson"));
        result.put("storySceneResultJson", context == null ? null : context.getAttribute("storySceneResultJson"));
        if (backgroundResult instanceof NodeResult backgroundNodeResult) {
            result.put("backgroundResult", backgroundNodeResult.getData());
            result.put("backgroundContent", backgroundNodeResult.getContent());
        } else {
            result.put("backgroundResult", backgroundResult);
        }
        return result;
    }
}
