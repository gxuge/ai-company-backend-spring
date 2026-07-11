package org.jeecg.modules.airag.agent.skill.runtime;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.model.SkillActivation;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.jeecg.modules.airag.agent.skill.model.SkillLoadResult;
import org.jeecg.modules.airag.agent.skill.prompt.SkillPromptBuilder;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.jeecg.modules.airag.agent.skill.router.SkillRouter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Skill 运行时编排服务。
 */
@Service
public class SkillRuntimeService {
    private final SkillRouter skillRouter;
    private final SkillPromptBuilder skillPromptBuilder;
    private final SkillRegistry skillRegistry;

    public SkillRuntimeService(SkillRouter skillRouter,
                               SkillPromptBuilder skillPromptBuilder,
                               SkillRegistry skillRegistry) {
        this.skillRouter = skillRouter;
        this.skillPromptBuilder = skillPromptBuilder;
        this.skillRegistry = skillRegistry;
    }

    /**
     * 准备 Skill 上下文。
     *
     * @param userInput 用户输入
     * @param domain 域
     * @param topK 候选数量
     * @return 准备结果
     */
    public SkillLoadResult prepare(String userInput, String domain, int topK) {
        List<SkillDefinition> candidates = this.skillRouter.route(userInput, domain, topK);
        List<SkillDefinition> safeCandidates = candidates == null ? new ArrayList<>() : candidates;
        String prompt = this.skillPromptBuilder.buildSkillIndexPrompt(safeCandidates);
        SkillActivation activation = buildActivation(safeCandidates);
        SkillLoadResult result = new SkillLoadResult();
        result.setDomain(domain);
        result.setTopK(topK);
        result.setCandidateSkills(safeCandidates);
        result.setSkillIndexPrompt(prompt);
        result.setActivation(activation);
        result.getMetadata().put("userInput", userInput);
        result.getMetadata().put("candidateCount", safeCandidates.size());
        return result;
    }

    /**
     * 读取 Skill 元信息。
     *
     * @param domain 域
     * @return 元信息
     */
    public List<SkillDefinition> listSkillIndex(String domain) {
        return this.skillRegistry.listSkillIndex(domain);
    }

    private SkillActivation buildActivation(List<SkillDefinition> candidates) {
        SkillActivation activation = new SkillActivation();
        if (candidates == null || candidates.isEmpty()) {
            activation.setLoadedSkillCodes(new ArrayList<>());
            activation.setAllowedTools(new ArrayList<>());
            return activation;
        }
        activation.setActiveSkillCode(candidates.get(0).getCode());
        activation.setLoadedSkillCodes(candidates.stream()
                .map(SkillDefinition::getCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        Set<String> allowedTools = new LinkedHashSet<>();
        for (SkillDefinition candidate : candidates) {
            if (candidate == null || candidate.getAllowedTools() == null) {
                continue;
            }
            allowedTools.addAll(candidate.getAllowedTools().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }
        activation.setAllowedTools(new ArrayList<>(allowedTools));
        return activation;
    }
}
