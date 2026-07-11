package org.jeecg.modules.airag.agent.skill.registry;

import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.jeecg.modules.airag.agent.skill.model.SkillResource;

import java.util.List;
import java.util.Optional;

/**
 * Skill 注册中心。
 */
public interface SkillRegistry {
    /**
     * 按域返回 Skill 索引。
     *
     * @param domain 任务域
     * @return 元信息列表
     */
    List<SkillDefinition> listSkillIndex(String domain);

    /**
     * 读取完整 Skill 内容。
     *
     * @param skillCode Skill 编码
     * @return 完整 Markdown
     */
    String getSkillBody(String skillCode);

    /**
     * 读取 Skill 资源。
     *
     * @param skillCode Skill 编码
     * @param resourcePath 资源相对路径
     * @return Skill 资源
     */
    Optional<SkillResource> getResource(String skillCode, String resourcePath);

    /**
     * 按编码读取 Skill 定义。
     *
     * @param skillCode Skill 编码
     * @return Skill 定义
     */
    Optional<SkillDefinition> findSkill(String skillCode);
}
