package org.jeecg.modules.airag.agent.interaction;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentFlowStateSupport;
import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.jeecg.modules.airag.agent.tool.options.AgentOptionsToolService;

import java.util.Map;

/**
 * 通用候选项等待与恢复支持。
 */
public final class AgentOptionsInteractionSupport {

    private AgentOptionsInteractionSupport() {
    }

    public static boolean isCandidateOptions(Map<String, Object> pending) {
        if (pending == null || pending.isEmpty()) {
            return false;
        }
        return AgentOptionsToolService.INTERACTION_TYPE.equalsIgnoreCase(
                oConvertUtils.getString(pending.get("interactionType"))
        ) && AgentOptionsToolService.TOOL_NAME.equalsIgnoreCase(
                oConvertUtils.getString(pending.get("toolName"))
        );
    }

    /**
     * 候选项已被点击或用户已自由输入时，恢复当前对话并清理待交互状态。
     *
     * @return true 表示可以继续执行 LLM；false 表示仍需等待用户
     */
    public static boolean resumeConversation(AgentContext context, Map<String, Object> pending) {
        String selectedValue = UserInteractionSupport.resolveSelectedValue(context, pending);
        String userInput = oConvertUtils.getString(context == null ? null : context.getUserInput());
        if (!oConvertUtils.isNotEmpty(selectedValue) && !oConvertUtils.isNotEmpty(userInput)) {
            return false;
        }
        if (!oConvertUtils.isNotEmpty(userInput) && oConvertUtils.isNotEmpty(selectedValue) && context != null) {
            context.setUserInput(resolveSelectedLabel(pending, selectedValue));
        }
        UserInteractionSupport.clear(context);
        return true;
    }

    public static AgentResult waitingResult(AgentContext context,
                                            Map<String, Object> pending,
                                            String resumeNode,
                                            String stage) {
        String question = oConvertUtils.getString(pending == null ? null : pending.get("question"));
        AgentFlowStateSupport.markResume(context, resumeNode, stage);
        AgentResult result = AgentResult.waitingUser(question);
        result.setStructuredResult(pending);
        result.getData().put("status", "WAITING_USER");
        result.getData().put("stage", stage);
        copy(result, pending, "question");
        copy(result, pending, "interactionId");
        copy(result, pending, "interactionType");
        copy(result, pending, "options");
        copy(result, pending, "suspendRun");
        AgentFlowStateSupport.attachResumeData(result, context);
        return result;
    }

    private static String resolveSelectedLabel(Map<String, Object> pending, String selectedValue) {
        Object rawOptions = pending == null ? null : pending.get("options");
        if (rawOptions instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (!(item instanceof Map<?, ?> option)) {
                    continue;
                }
                String value = firstText(option, "optionValue", "value");
                if (selectedValue.equals(value)) {
                    String label = firstText(option, "label", "text", "name");
                    return oConvertUtils.isNotEmpty(label) ? label : selectedValue;
                }
            }
        }
        return selectedValue;
    }

    private static String firstText(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            String value = oConvertUtils.getString(source.get(key));
            if (oConvertUtils.isNotEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private static void copy(AgentResult result, Map<String, Object> source, String field) {
        if (result != null && source != null && source.get(field) != null) {
            result.getData().put(field, source.get(field));
        }
    }
}
