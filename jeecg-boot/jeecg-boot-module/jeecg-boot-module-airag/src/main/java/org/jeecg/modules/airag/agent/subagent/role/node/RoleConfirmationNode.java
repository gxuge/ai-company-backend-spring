package org.jeecg.modules.airag.agent.subagent.role.node;

import org.jeecg.modules.airag.agent.node.ConfirmationNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 角色确认节点。
 *
 * <p>负责展示角色确认选项，并根据前端回传的 optionValue 输出确定性流程动作。</p>
 *
 * @author codex
 * @date 2026/7/14
 */
@Component
public class RoleConfirmationNode extends ConfirmationNode {

    public RoleConfirmationNode() {
        super(
                "role_confirmation",
                "角色确认",
                "confirm",
                "你对这版角色满意吗？",
                List.of(
                        Map.of("label", "满意，继续生成", "value", "ACCEPT_AND_CONTINUE"),
                        Map.of("label", "不满意，重新生成", "value", "REGENERATE")
                ),
                DEFAULT_OPTION_VALUE_ATTRIBUTE
        );
    }

    @Override
    protected String resolveAction(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            return ACTION_WAIT_CONFIRM;
        }
        String action = optionValue.trim().toUpperCase(Locale.ROOT);
        if ("ACCEPT_AND_CONTINUE".equals(action)
                || "REGENERATE".equals(action)
                || "MODIFY".equals(action)) {
            return action;
        }
        return ACTION_ASK_USER;
    }

    @Override
    protected String buildReply(String action) {
        if ("ACCEPT_AND_CONTINUE".equals(action)) {
            return "好的，我继续为这个角色生成形象和声音。";
        }
        if ("REGENERATE".equals(action)) {
            return "好的，我帮你重新生成一版角色。";
        }
        if ("MODIFY".equals(action)) {
            return "好的，我会按你的修改意见调整这版角色。";
        }
        return getQuestion();
    }
}
