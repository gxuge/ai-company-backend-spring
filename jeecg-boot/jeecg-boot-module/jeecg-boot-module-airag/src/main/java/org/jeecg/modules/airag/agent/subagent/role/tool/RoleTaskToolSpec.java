package org.jeecg.modules.airag.agent.subagent.role.tool;

import java.util.List;

/**
 * 角色子 Agent 预留工具清单。
 *
 * <p>这里只保存未来会接入的工具名，暂不实现任何业务执行逻辑。</p>
 *
 * @author codex
 * @date 2026/7/10
 */
public final class RoleTaskToolSpec {

    /**
     * 预留工具名。
     */
    public static final String ROLE_CORE_FILL = "role_core_fill";
    /**
     * 预留工具名。
     */
    public static final String ROLE_REQUEST_CONFIRMATION = "role_request_confirmation";
    /**
     * 用户确认满意后显式继续生成角色形象和声音。
     */
    public static final String ROLE_GENERATE_COMPLETE = "role_generate_complete";
    /**
     * 预留工具名。
     */
    public static final String ROLE_CORE_FILL_PRESET = "role_core_fill_preset";
    /**
     * 预留工具名。
     */
    public static final String ROLE_GENERATE_ROLE = "role_generate_role";
    /**
     * 预留工具名。
     */
    public static final String ROLE_GENERATE_ROLE_IMAGE = "role_generate_role_image";
    /**
     * 预留工具名。
     */
    public static final String ROLE_GENERATE_ROLE_VOICE = "role_generate_role_voice";

    /**
     * 当前角色链路的预留工具列表。
     */
    public static final List<String> TOOL_NAMES = List.of(
            ROLE_CORE_FILL,
            ROLE_REQUEST_CONFIRMATION,
            ROLE_GENERATE_COMPLETE,
            ROLE_CORE_FILL_PRESET,
            ROLE_GENERATE_ROLE,
            ROLE_GENERATE_ROLE_IMAGE,
            ROLE_GENERATE_ROLE_VOICE
    );

    private RoleTaskToolSpec() {
    }
}
