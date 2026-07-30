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
            return "None";
        }
        return this.registry.values().stream()
                .filter(subAgent -> subAgent != null && !"welcome_intro".equalsIgnoreCase(subAgent.subAgentName()))
                .map(subAgent -> subAgent.subAgentName() + ": " + describeSubAgent(subAgent.subAgentName()))
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
            return "Unnamed sub-agent";
        }
        return switch (subAgentName) {
            case "role_task_agent" ->
                    "Creates complete roles. Use when the user asks to create, design, or refine a role, or asks AI to decide role details. It gathers the role name, gender, occupation, and background story through conversation, then generates the complete role after confirmation. Do not use for image-only or voice-only requests.";
            case "story_task_agent" ->
                    "Creates complete stories. Use when the user asks to create, write, or refine a story, or asks AI to decide story details. It gathers the title, world setting, scene setting, plot outline, and role information through conversation, then generates the complete story after confirmation. Do not use for background-image-only requests.";
            case "role_image_task_agent" ->
                    "Creates role images. Use for role appearance design, portraits, avatars, full-body images, or other role image requests. Delegate even when appearance details are incomplete; this sub-agent gathers appearance, clothing, and overall presence through conversation before generating the image. Do not use for complete role creation or voice-only requests.";
            case "story_background_task_agent" ->
                    "Creates story scene background images. Use for story backgrounds, scene images, environment concept art, or other background image requests. Delegate even when scene details are incomplete; this sub-agent gathers the core location, atmosphere, and key features through conversation before generating the image. Do not use for complete story creation.";
            default -> subAgentName;
        };
    }
}
