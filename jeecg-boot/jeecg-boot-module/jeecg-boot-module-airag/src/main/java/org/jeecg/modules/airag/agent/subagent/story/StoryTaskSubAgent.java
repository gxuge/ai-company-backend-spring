package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.interaction.AgentOptionsInteractionSupport;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryGenerateCompleteToolContract;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事子 Agent。
 *
 * <p>故事对话节点通过 Tool 展示确认，用户明确同意后提交完整故事异步生成任务。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryTaskSubAgent implements SubAgent {
    private static final String STAGE_DIALOG = "dialog";
    private static final String STAGE_CONFIRMATION = "confirmation";

    private final NodeRunner nodeRunner;
    private final StoryCreateDialogNode storyCreateDialogNode;

    public StoryTaskSubAgent(NodeRunner nodeRunner,
                             StoryCreateDialogNode storyCreateDialogNode) {
        this.nodeRunner = nodeRunner;
        this.storyCreateDialogNode = storyCreateDialogNode;
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

            return continueWithDialog(context, chainData);
        } catch (Exception ex) {
            AgentResult result = AgentErrorSupport.failed(
                    AgentErrorCode.RUNTIME_SUBAGENT_EXECUTION_FAILED,
                    Map.of("subAgentName", subAgentName())
            );
            AgentErrorSupport.attach(result, ex, AgentErrorCode.RUNTIME_SUBAGENT_EXECUTION_FAILED);
            result.getData().putAll(chainData);
            result.getData().put("stage", "failed");
            AgentFlowStateSupport.attachResumeData(result, context);
            return result;
        }
    }

    /**
     * 处理 Tool 创建的展示交互；将选项映射为内部确认状态后重新进入故事对话。
     */
    private AgentResult handlePendingInteraction(AgentContext context,
                                                 Map<String, Object> chainData,
                                                 Map<String, Object> pendingInteraction) {
        if (AgentOptionsInteractionSupport.isCandidateOptions(pendingInteraction)) {
            if (AgentOptionsInteractionSupport.resumeConversation(context, pendingInteraction)) {
                return null;
            }
            AgentResult result = AgentOptionsInteractionSupport.waitingResult(
                    context,
                    pendingInteraction,
                    this.storyCreateDialogNode.nodeName(),
                    STAGE_DIALOG
            );
            result.getData().putAll(chainData);
            return result;
        }
        String selectedValue = UserInteractionSupport.resolveSelectedValue(context, pendingInteraction);
        if ((context == null || !oConvertUtils.isNotEmpty(context.getUserInput()))
                && !oConvertUtils.isNotEmpty(selectedValue)) {
            return waitingInteraction(context, chainData, pendingInteraction);
        }
        if (oConvertUtils.isNotEmpty(selectedValue)) {
            StoryConfirmationTransitions.applySelectedValue(context, selectedValue);
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
        if (StoryGenerateCompleteToolContract.consumeAccepted(context)) {
            return completeAfterGenerationAccepted(context, chainData);
        }

        Map<String, Object> pendingInteraction = UserInteractionSupport.getPending(context);
        if (!pendingInteraction.isEmpty()) {
            if (AgentOptionsInteractionSupport.isCandidateOptions(pendingInteraction)) {
                AgentResult result = AgentOptionsInteractionSupport.waitingResult(
                        context,
                        pendingInteraction,
                        this.storyCreateDialogNode.nodeName(),
                        STAGE_DIALOG
                );
                result.getData().putAll(chainData);
                return result;
            }
            return waitingInteraction(context, chainData, pendingInteraction);
        }
        return waiting(
                context,
                dialogResult == null ? null : dialogResult.getContent(),
                chainData,
                STAGE_DIALOG
        );
    }

    private AgentResult completeAfterGenerationAccepted(AgentContext context,
                                                        Map<String, Object> chainData) {
        String content = "Story generation has started";
        if (context != null) {
            context.putAttribute("storyTaskStage", "done");
        }
        StoryConfirmationTransitions.clearDecision(context);
        Object structuredResult = buildStructuredResult(context);
        AgentResult result = AgentHandoffSupport.buildTerminalCompletedHandoffResult(
                context,
                subAgentName(),
                content,
                structuredResult
        );
        result.getData().putAll(chainData);
        result.getData().put("stage", "done");
        result.getData().put("generationStatus", "running");
        return result;
    }

    private AgentResult waiting(AgentContext context, String content, Map<String, Object> chainData, String stage) {
        String text = oConvertUtils.isNotEmpty(content) ? content : "Please continue describing the story.";
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
            question = "Are you satisfied with this story?";
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

    private Map<String, Object> buildStructuredResult(AgentContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storyCoreResultJson", context == null ? null : context.getAttribute("storyCoreResultJson"));
        result.put(
                StoryGenerateCompleteToolContract.TRANSFER_DATA_JSON,
                context == null ? null : context.getAttribute(StoryGenerateCompleteToolContract.TRANSFER_DATA_JSON)
        );
        result.put(
                "taskId",
                context == null ? null : context.getAttribute(StoryGenerateCompleteToolContract.ATTR_GENERATION_TASK_ID)
        );
        result.put(
                "eventId",
                context == null ? null : context.getAttribute(StoryGenerateCompleteToolContract.ATTR_GENERATION_EVENT_ID)
        );
        result.put("status", "running");
        return result;
    }
}
