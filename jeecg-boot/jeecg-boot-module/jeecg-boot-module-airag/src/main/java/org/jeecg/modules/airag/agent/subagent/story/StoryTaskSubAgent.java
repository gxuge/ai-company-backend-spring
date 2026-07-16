package org.jeecg.modules.airag.agent.subagent.story;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryConfirmationNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateBackgroundNode;
import org.jeecg.modules.airag.agent.subagent.story.node.StoryCreateDialogNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 故事子 Agent。
 *
 * <p>执行顺序：故事对话 -> 用户确认 -> 故事背景 / 场景。</p>
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
    private final StoryConfirmationNode storyConfirmationNode;
    private final StoryCreateBackgroundNode storyCreateBackgroundNode;

    public StoryTaskSubAgent(NodeRunner nodeRunner,
                             StoryCreateDialogNode storyCreateDialogNode,
                             StoryConfirmationNode storyConfirmationNode,
                             StoryCreateBackgroundNode storyCreateBackgroundNode) {
        this.nodeRunner = nodeRunner;
        this.storyCreateDialogNode = storyCreateDialogNode;
        this.storyConfirmationNode = storyConfirmationNode;
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
            String stage = resolveStage(context);
            if (STAGE_BACKGROUND.equals(stage)) {
                return continueWithBackground(context, chainData);
            }
            if ((STAGE_CONFIRMATION.equals(stage) || hasStoryCoreState(context)) && hasStoryCoreState(context)) {
                AgentResult decisionResult = handleConfirmationOption(context, chainData);
                if (decisionResult != null) {
                    return decisionResult;
                }
            }

            markStage(context, STAGE_DIALOG, this.storyCreateDialogNode.nodeName());
            NodeResult dialogResult = this.nodeRunner.run(context, this.storyCreateDialogNode);
            storeNodeResult(context, "storyDialogNodeResult", dialogResult);
            AgentResult dialogHandoff = handoffIfNeeded(context, dialogResult, chainData, "dialog");
            if (dialogHandoff != null) {
                return dialogHandoff;
            }

            if (!hasStoryCoreState(context)) {
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
        markStage(context, STAGE_CONFIRMATION, this.storyConfirmationNode.nodeName());
        NodeResult confirmationResult = this.nodeRunner.run(context, this.storyConfirmationNode);
        storeNodeResult(context, "storyConfirmationNodeResult", confirmationResult);
        AgentResult handoff = handoffIfNeeded(context, confirmationResult, chainData, STAGE_CONFIRMATION);
        if (handoff != null) {
            return handoff;
        }
        Map<String, Object> decision = extractDecision(confirmationResult);
        if (context != null) {
            this.storyConfirmationNode.consumeOptionValue(context);
            context.putAttribute("storyConfirmationDecision", decision);
        }
        String action = oConvertUtils.getString(decision == null ? null : decision.get("action"));
        if ("ACCEPT_AND_CONTINUE".equalsIgnoreCase(action)) {
            return continueWithBackground(context, chainData);
        }
        if ("REGENERATE".equalsIgnoreCase(action) || "MODIFY".equalsIgnoreCase(action)) {
            return null;
        }
        String content = oConvertUtils.getString(decision == null ? null : decision.get("question"));
        if (!oConvertUtils.isNotEmpty(content)) {
            content = oConvertUtils.getString(decision == null ? null : decision.get("reply"));
        }
        if (!oConvertUtils.isNotEmpty(content)) {
            content = confirmationResult == null ? null : confirmationResult.getContent();
        }
        return waiting(context, content, chainData, STAGE_CONFIRMATION, decision);
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
        return waiting(context, content, chainData, stage, null);
    }

    private AgentResult waiting(AgentContext context,
                                String content,
                                Map<String, Object> chainData,
                                String stage,
                                Map<String, Object> decision) {
        String text = oConvertUtils.isNotEmpty(content) ? content : "你对这版故事满意吗？想先改哪部分？";
        String resumeNodeName = STAGE_CONFIRMATION.equals(stage)
                ? this.storyConfirmationNode.nodeName()
                : this.storyCreateDialogNode.nodeName();
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
            stage = oConvertUtils.getString(context.getAttribute("storyTaskStage"));
        }
        if (!oConvertUtils.isNotEmpty(stage)
                && this.storyCreateBackgroundNode.nodeName().equalsIgnoreCase(
                oConvertUtils.getString(context.getResumeNodeName()))) {
            return STAGE_BACKGROUND;
        }
        if (!oConvertUtils.isNotEmpty(stage)) {
            return hasStoryCoreState(context) ? STAGE_CONFIRMATION : STAGE_DIALOG;
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

    private boolean hasStoryCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("storyCoreResultJson") != null
                || context.getAttribute("storyCorePresetResultJson") != null
                || context.getAttribute("storyFullGenerateResultJson") != null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDecision(Object nodeResult) {
        if (!(nodeResult instanceof NodeResult result)) {
            return new LinkedHashMap<>();
        }
        Object toolData = result.getData().get("toolData");
        if (toolData instanceof Map<?, ?> rawMap) {
            return copyStringKeyMap(rawMap);
        }
        Object confirmationDecision = result.getData().get("confirmationDecision");
        if (confirmationDecision instanceof Map<?, ?> rawMap) {
            return copyStringKeyMap(rawMap);
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

    private Map<String, Object> buildStructuredResult(AgentContext context, Object backgroundResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storyCoreResultJson", context == null ? null : context.getAttribute("storyCoreResultJson"));
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
