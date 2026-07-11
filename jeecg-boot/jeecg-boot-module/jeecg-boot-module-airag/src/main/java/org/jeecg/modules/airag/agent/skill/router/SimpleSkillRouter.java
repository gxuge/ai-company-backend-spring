package org.jeecg.modules.airag.agent.skill.router;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 简单 Skill 路由器。
 */
@Component
public class SimpleSkillRouter implements SkillRouter {
    private final SkillRegistry skillRegistry;

    public SimpleSkillRouter(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @Override
    public List<SkillDefinition> route(String userInput, String domain, int topK) {
        int safeTopK = topK <= 0 ? 3 : topK;
        List<SkillDefinition> skills = this.skillRegistry.listSkillIndex(domain);
        if (skills.isEmpty()) {
            return skills;
        }
        List<ScoredSkill> scored = new ArrayList<>(skills.size());
        for (SkillDefinition skill : skills) {
            int score = score(userInput, skill);
            scored.add(new ScoredSkill(skill, score));
        }
        scored.sort(Comparator
                .comparingInt(ScoredSkill::score).reversed()
                .thenComparing(item -> oConvertUtils.getString(item.skill().getCode()).toLowerCase(Locale.ROOT)));

        List<SkillDefinition> selected = new ArrayList<>();
        for (ScoredSkill item : scored) {
            if (selected.size() >= safeTopK) {
                break;
            }
            selected.add(item.skill().copyWithoutContent());
        }
        if (selected.isEmpty()) {
            for (int i = 0; i < Math.min(safeTopK, skills.size()); i++) {
                selected.add(skills.get(i).copyWithoutContent());
            }
        }
        return selected;
    }

    private int score(String userInput, SkillDefinition skill) {
        if (!StringUtils.hasText(userInput) || skill == null) {
            return 0;
        }
        String text = userInput.toLowerCase(Locale.ROOT);
        Set<String> keywords = new LinkedHashSet<>();
        addKeywords(keywords, skill.getCode());
        addKeywords(keywords, skill.getName());
        addKeywords(keywords, skill.getDescription());
        int score = 0;
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += Math.max(1, Math.min(5, keyword.length() / 2));
            }
        }
        return score;
    }

    private void addKeywords(Set<String> keywords, String source) {
        if (!StringUtils.hasText(source)) {
            return;
        }
        String normalized = source.trim();
        for (String part : normalized.split("[\\s,，;；/|]+")) {
            String keyword = part == null ? null : part.trim();
            if (StringUtils.hasText(keyword)) {
                keywords.add(keyword);
            }
        }
    }

    private record ScoredSkill(SkillDefinition skill, int score) {
    }
}
