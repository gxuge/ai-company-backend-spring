package org.jeecg.modules.airag.agent.common;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.SubAgent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 子 Agent 注册中心。
 *
 * @author codex
 * @date 2026/6/25
 */
@Component
@RequiredArgsConstructor
public class SubAgentRegistry {
    /**
     * Spring 容器中的子 Agent 列表。
     */
    private final List<SubAgent> subAgents;
    /**
     * 名称到实例的索引。
     */
    private final Map<String, SubAgent> registry = new LinkedHashMap<>();

    /**
     * 初始化索引。
     */
    @PostConstruct
    public void init() {
        this.registry.clear();
        if (this.subAgents == null) {
            return;
        }
        for (SubAgent subAgent : this.subAgents) {
            if (subAgent == null || oConvertUtils.isEmpty(subAgent.subAgentName())) {
                continue;
            }
            this.registry.put(subAgent.subAgentName(), subAgent);
        }
    }

    /**
     * 根据名称查找子 Agent。
     *
     * @param subAgentName 子 Agent 名称
     * @return 子 Agent
     */
    public Optional<SubAgent> find(String subAgentName) {
        if (oConvertUtils.isEmpty(subAgentName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.registry.get(subAgentName));
    }

    /**
     * 获取默认子 Agent 名称。
     *
     * @return 默认名称
     */
    public String defaultSubAgentName() {
        return this.registry.keySet().stream().findFirst().orElse("");
    }

    /**
     * 判断是否已注册。
     *
     * @param subAgentName 名称
     * @return 是否存在
     */
    public boolean exists(String subAgentName) {
        return this.registry.containsKey(subAgentName);
    }

    /**
     * 生成可读的子 Agent 列表。
     *
     * @return 文本列表
     */
    public String describeAvailableSubAgents() {
        if (this.registry.isEmpty()) {
            return "无";
        }
        return this.registry.values().stream()
                .filter(subAgent -> subAgent != null && !"welcome_intro".equalsIgnoreCase(subAgent.subAgentName()))
                .map(subAgent -> subAgent.subAgentName() + "：" + describeSubAgent(subAgent.subAgentName()))
                .distinct()
                .collect(Collectors.joining("\n", "- ", ""));
    }

    /**
     * 生成子 Agent 的可读说明。
     *
     * @param subAgentName 名称
     * @return 说明文本
     */
    private String describeSubAgent(String subAgentName) {
        if (oConvertUtils.isEmpty(subAgentName)) {
            return "未命名子 Agent";
        }
        return switch (subAgentName) {
            case "role_task_agent" -> "角色创建对话，支持追问、preset/full 生成、确认后补形象与声音";
            case "story_task_agent" -> "故事创建对话，支持追问、preset/full 生成、确认后补背景与场景";
            case "role_image_task_agent" -> "生成角色形象图";
            case "story_scene_image_task_agent" -> "生成故事场景图";
            default -> subAgentName;
        };
    }
}
