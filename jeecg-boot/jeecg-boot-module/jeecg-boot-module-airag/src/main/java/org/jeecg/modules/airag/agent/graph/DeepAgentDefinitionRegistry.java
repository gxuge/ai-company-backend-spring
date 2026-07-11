package org.jeecg.modules.airag.agent.graph;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deep Agent 定义注册中心。
 *
 * 当前用于主 Agent 规划、子 Agent 路由与上下文注入。
 */
@Component
public class DeepAgentDefinitionRegistry {
    /**
     * 子 Agent 定义缓存。
     */
    private final Map<String, DeepAgentDefinition> registry = new LinkedHashMap<>();

    /**
     * 初始化默认定义。
     */
    @PostConstruct
    public void init() {
        this.registry.clear();
        register(buildGeneralChatDefinition());
        register(buildRoleDefinition());
        register(buildStoryDefinition());
    }

    /**
     * 查找定义。
     *
     * @param name 子 Agent 名称
     * @return 定义
     */
    public Optional<DeepAgentDefinition> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.registry.get(name));
    }

    /**
     * 获取定义，不存在时抛异常。
     *
     * @param name 子 Agent 名称
     * @return 定义
     */
    public DeepAgentDefinition require(String name) {
        return find(name).orElseThrow(() -> new IllegalStateException("未找到 Deep Agent 定义: " + name));
    }

    /**
     * 获取全部定义。
     *
     * @return 定义列表
     */
    public List<DeepAgentDefinition> list() {
        return new ArrayList<>(this.registry.values());
    }

    /**
     * 默认 Deep Agent 名称。
     *
     * @return 默认名称
     */
    public String defaultDeepAgentName() {
        if (this.registry.containsKey("general_chat")) {
            return "general_chat";
        }
        return this.registry.keySet().stream().findFirst().orElse("general_chat");
    }

    /**
     * 生成可读的 Deep Agent 列表。
     *
     * @return 列表文本
     */
    public String describeAvailableDeepAgents() {
        if (this.registry.isEmpty()) {
            return "无";
        }
        return this.registry.values().stream()
                .map(this::describe)
                .distinct()
                .collect(Collectors.joining("\n", "- ", ""));
    }

    private void register(DeepAgentDefinition definition) {
        if (definition == null || definition.getName() == null || definition.getName().isBlank()) {
            return;
        }
        this.registry.put(definition.getName(), definition);
    }

    private DeepAgentDefinition buildGeneralChatDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("general_chat");
        definition.setDescription("普通聊天、陪伴对话、闲聊回复。");
        definition.setSkillDomain("chat");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("general_chat_reply"));
        definition.setTools(List.of());
        definition.setPermissions(List.of());
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "general-chat");
        definition.getMetadata().put("mode", "deep-agents");
        return definition;
    }

    private DeepAgentDefinition buildRoleDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("role_task_agent");
        definition.setDescription("创建角色对话子 Agent，支持追问、preset/full 生成、确认后补形象与声音。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_dialog", "role_create_image", "role_create_voice"));
        definition.setTools(List.of(
                "role_flow_gate",
                "role_core_fill_preset",
                "role_generate_role",
                "role_generate_role_image",
                "role_generate_role_voice"
        ));
        definition.setPermissions(List.of(
                "role_flow_gate",
                "role_core_fill_preset",
                "role_generate_role",
                "role_generate_role_image",
                "role_generate_role_voice"
        ));
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "create-role");
        definition.getMetadata().put("mode", "deep-agents");
        definition.getMetadata().put("chain", List.of("role_create_dialog", "role_create_image", "role_create_voice"));
        return definition;
    }

    private DeepAgentDefinition buildStoryDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("story_task_agent");
        definition.setDescription("创建故事对话子 Agent，支持追问、preset/full 生成与结果确认。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_dialog"));
        definition.setTools(List.of(
                "story_full_generate_preset",
                "story_full_generate"
        ));
        definition.setPermissions(List.of(
                "story_full_generate_preset",
                "story_full_generate"
        ));
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("mode", "deep-agents");
        return definition;
    }

    /**
     * 生成单个定义的可读说明。
     */
    private String describe(DeepAgentDefinition definition) {
        if (definition == null) {
            return "未命名 Deep Agent";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(definition.getName());
        if (definition.getDescription() != null && !definition.getDescription().isBlank()) {
            builder.append("：").append(definition.getDescription());
        }
        if (definition.getSkills() != null && !definition.getSkills().isEmpty()) {
            builder.append(" | skills=").append(definition.getSkills());
        }
        if (definition.getTools() != null && !definition.getTools().isEmpty()) {
            builder.append(" | tools=").append(definition.getTools());
        }
        return builder.toString();
    }
}
