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
        register(buildRoleDefinition());
        register(buildStoryDefinition());
        register(buildRoleImageDefinition());
        register(buildStoryBackgroundDefinition());
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
        return this.registry.keySet().stream().findFirst().orElse("");
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

    private DeepAgentDefinition buildRoleDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("role_task_agent");
        definition.setDescription("创建角色对话子 Agent，支持追问、角色生成、确认后补形象与声音。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("role_create_dialog", "role_create_image", "role_create_voice"));
        definition.setTools(List.of(
                "role_generate_role",
                "role_generate_role_image",
                "role_generate_role_voice"
        ));
        definition.setPermissions(List.of(
                "role_generate_role",
                "role_generate_role_image",
                "role_generate_role_voice"
        ));
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "create-role");
        definition.getMetadata().put("mode", "deep-agents");
        definition.getMetadata().put(
                "chain",
                List.of("role_create_dialog", "role_confirmation", "role_create_image", "role_create_voice")
        );
        return definition;
    }

    private DeepAgentDefinition buildStoryDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("story_task_agent");
        definition.setDescription("创建故事对话子 Agent，支持追问、故事生成、确认后补背景与场景。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_dialog", "story_create_background"));
        definition.setTools(List.of(
                "story_full_generate",
                "story_generate_scene"
        ));
        definition.setPermissions(List.of(
                "story_full_generate",
                "story_generate_scene"
        ));
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("mode", "deep-agents");
        definition.getMetadata().put(
                "chain",
                List.of("story_create_dialog", "story_confirmation", "story_create_background")
        );
        return definition;
    }

    private DeepAgentDefinition buildRoleImageDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("role_image_task_agent");
        definition.setDescription("根据用户提供的角色外观、身份或已有角色设定，直接生成角色形象。");
        definition.setSkillDomain("role");
        definition.setSkillTopK(1);
        definition.setSkills(List.of("role_create_image"));
        definition.setTools(List.of("role_generate_role_image"));
        definition.setPermissions(List.of("role_generate_role_image"));
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "create-role-image");
        definition.getMetadata().put("mode", "deep-agents");
        definition.getMetadata().put("chain", List.of("role_create_image"));
        return definition;
    }

    private DeepAgentDefinition buildStoryBackgroundDefinition() {
        DeepAgentDefinition definition = new DeepAgentDefinition();
        definition.setName("story_background_task_agent");
        definition.setDescription("根据用户提供的故事方向或已有故事设定，直接生成故事背景与场景。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(1);
        definition.setSkills(List.of("story_create_background"));
        definition.setTools(List.of("story_generate_scene"));
        definition.setPermissions(List.of("story_generate_scene"));
        definition.setResponseFormat("text");
        definition.getMetadata().put("flow", "create-story-background");
        definition.getMetadata().put("mode", "deep-agents");
        definition.getMetadata().put("chain", List.of("story_create_background"));
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
