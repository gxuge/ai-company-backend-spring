package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentHandoffSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.runtime.NodeRunner;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateDialogNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateImageNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleCreateVoiceNode;
import org.jeecg.modules.airag.agent.subagent.role.node.RoleFlowGateNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 角色子 Agent。
 *
 * <p>执行顺序：角色对话 -> 流程门禁 -> 形象 -> 声音。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
@Component
public class RoleTaskSubAgent implements SubAgent {

    private final NodeRunner nodeRunner;
    private final RoleCreateDialogNode roleCreateDialogNode;
    private final RoleFlowGateNode roleFlowGateNode;
    private final RoleCreateImageNode roleCreateImageNode;
    private final RoleCreateVoiceNode roleCreateVoiceNode;

    public RoleTaskSubAgent(NodeRunner nodeRunner,
                            RoleCreateDialogNode roleCreateDialogNode,
                            RoleFlowGateNode roleFlowGateNode,
                            RoleCreateImageNode roleCreateImageNode,
                            RoleCreateVoiceNode roleCreateVoiceNode) {
        this.nodeRunner = nodeRunner;
        this.roleCreateDialogNode = roleCreateDialogNode;
        this.roleFlowGateNode = roleFlowGateNode;
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
            context.putAttribute("roleTaskStage", "dialog");
        }

        try {
            if (hasRoleCoreState(context)) {
                AgentResult decisionResult = handleExistingRoleCore(context, chainData);
                if (decisionResult != null) {
                    return decisionResult;
                }
            }

            var dialogResult = this.nodeRunner.run(context, this.roleCreateDialogNode);
            storeNodeResult(context, "roleDialogNodeResult", dialogResult);
            AgentResult dialogHandoff = handoffIfNeeded(context, dialogResult, chainData, "dialog");
            if (dialogHandoff != null) {
                return dialogHandoff;
            }

            if (!hasRoleCoreState(context)) {
                return waiting(dialogResult == null ? null : dialogResult.getContent(), chainData, "dialog");
            }

            if (context != null) {
                context.putAttribute("roleTaskStage", "gate");
            }
            var gateResult = this.nodeRunner.run(context, this.roleFlowGateNode);
            storeNodeResult(context, "roleFlowGateNodeResult", gateResult);
            AgentResult gateHandoff = handoffIfNeeded(context, gateResult, chainData, "gate");
            if (gateHandoff != null) {
                return gateHandoff;
            }
            Map<String, Object> gateDecision = extractDecision(gateResult);
            if (context != null) {
                context.putAttribute("roleFlowGateDecision", gateDecision);
            }
            if (!shouldContinue(gateDecision)) {
                String question = oConvertUtils.getString(gateDecision == null ? null : gateDecision.get("question"));
                if (!oConvertUtils.isNotEmpty(question)) {
                    question = gateResult == null ? null : gateResult.getContent();
                }
                return waiting(question, chainData, "gate");
            }

            return continueWithImageAndVoice(context, chainData);
        } catch (Exception ex) {
            AgentResult result = AgentResult.failed(ex.getMessage());
            result.getData().putAll(chainData);
            result.getData().put("stage", "failed");
            return result;
        }
    }

    private AgentResult handleExistingRoleCore(AgentContext context, Map<String, Object> chainData) {
        if (context != null) {
            context.putAttribute("roleTaskStage", "confirmation");
        }
        var dialogResult = this.nodeRunner.run(context, this.roleCreateDialogNode);
        storeNodeResult(context, "roleConfirmationDialogNodeResult", dialogResult);
        AgentResult handoff = handoffIfNeeded(context, dialogResult, chainData, "confirmation");
        if (handoff != null) {
            return handoff;
        }
        Map<String, Object> decision = extractDecision(dialogResult);
        if (context != null) {
            context.putAttribute("roleConfirmationDecision", decision);
        }
        String action = oConvertUtils.getString(decision.get("action"));
        if ("ACCEPT_AND_CONTINUE".equalsIgnoreCase(action)) {
            return continueWithImageAndVoice(context, chainData);
        }
        if ("ASK_USER".equalsIgnoreCase(action)) {
            String reply = oConvertUtils.getString(decision.get("reply"));
            return waiting(reply, chainData, "confirmation", decision);
        }
        if ("REGENERATE".equalsIgnoreCase(action) || "MODIFY".equalsIgnoreCase(action)) {
            if (context != null) {
                context.putAttribute("roleConfirmationAction", action);
            }
            return null;
        }
        return waiting(oConvertUtils.getString(decision.get("reply")), chainData, "confirmation", decision);
    }

    private AgentResult continueWithImageAndVoice(AgentContext context, Map<String, Object> chainData) {
        if (context != null) {
            context.putAttribute("roleTaskStage", "image");
        }
        var imageResult = this.nodeRunner.run(context, this.roleCreateImageNode);
        storeNodeResult(context, "roleImageNodeResult", imageResult);
        AgentResult imageHandoff = handoffIfNeeded(context, imageResult, chainData, "image");
        if (imageHandoff != null) {
            return imageHandoff;
        }

        if (context != null) {
            context.putAttribute("roleTaskStage", "voice");
        }
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
        AgentResult result = AgentResult.success(content);
        result.setStructuredResult(buildStructuredResult(context, imageResult, voiceResult));
        result.getData().putAll(chainData);
        result.getData().put("stage", "done");
        result.getData().put("image", imageResult == null ? null : imageResult.getData());
        result.getData().put("voice", voiceResult == null ? null : voiceResult.getData());
        return result;
    }

    private AgentResult waiting(String content, Map<String, Object> chainData, String stage) {
        return waiting(content, chainData, stage, null);
    }

    private AgentResult waiting(String content, Map<String, Object> chainData, String stage, Map<String, Object> decision) {
        String text = oConvertUtils.isNotEmpty(content) ? content : "你对这版角色满意吗？想先改哪部分？";
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
        return result;
    }

    private boolean shouldContinue(Map<String, Object> gateDecision) {
        if (gateDecision == null || gateDecision.isEmpty()) {
            return true;
        }
        Object action = gateDecision.get("action");
        return action == null || "NEXT".equalsIgnoreCase(String.valueOf(action));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDecision(Object nodeResult) {
        if (!(nodeResult instanceof org.jeecg.modules.airag.agent.graph.NodeResult result)) {
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
