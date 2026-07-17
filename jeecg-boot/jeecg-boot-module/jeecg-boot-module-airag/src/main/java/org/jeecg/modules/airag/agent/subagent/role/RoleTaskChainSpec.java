package org.jeecg.modules.airag.agent.subagent.role;

import java.util.List;

/**
 * 角色子 Agent 的链路占位规格。
 *
 * <p>当前仅用于声明后续会接入的节点与工具名称，暂不承载任何业务逻辑。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
public final class RoleTaskChainSpec {

    /**
     * 子 Agent 名称。
     */
    public static final String SUB_AGENT_NAME = "role_task_agent";

    /**
     * 预留的 skill 顺序。
     */
    public static final List<String> SKILLS = List.of(
            "role_create_dialog",
            "role_create_image",
            "role_create_voice"
    );

    /**
     * 预留的工具顺序。
     */
    public static final List<String> TOOLS = List.of(
            "role_core_fill",
            "role_request_confirmation",
            "role_generate_role_image",
            "role_generate_role_voice"
    );

    /**
     * 预留的链路阶段。
     */
    public static final List<String> CHAIN = List.of(
            "role_create_dialog",
            "role_create_image",
            "role_create_voice"
    );

    private RoleTaskChainSpec() {
    }
}
