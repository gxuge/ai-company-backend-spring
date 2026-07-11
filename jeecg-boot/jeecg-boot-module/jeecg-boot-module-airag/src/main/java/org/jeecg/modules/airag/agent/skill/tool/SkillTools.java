package org.jeecg.modules.airag.agent.skill.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.guard.SkillToolPolicy;
import org.jeecg.modules.airag.agent.skill.model.SkillActivation;
import org.jeecg.modules.airag.agent.skill.model.SkillResource;
import org.jeecg.modules.airag.agent.skill.registry.SkillRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill 相关工具。
 */
@Component
public class SkillTools {
    private static final String TOOL_READ_SKILL = "readSkill";
    private static final String TOOL_READ_SKILL_RESOURCE = "readSkillResource";

    private final SkillRegistry skillRegistry;
    private final SkillToolPolicy skillToolPolicy;

    public SkillTools(SkillRegistry skillRegistry,
                      SkillToolPolicy skillToolPolicy) {
        this.skillRegistry = skillRegistry;
        this.skillToolPolicy = skillToolPolicy;
    }

    /**
     * 读取已注册 Skill 的完整正文。
     *
     * @param skillCode Skill 编码
     * @return 完整 SKILL.md
     */
    @Tool("读取已注册 skill 的完整 SKILL.md 正文")
    public String readSkill(String skillCode) {
        if (!StringUtils.hasText(skillCode)) {
            return "Skill 读取失败：skillCode不能为空";
        }
        if (!this.skillToolPolicy.isToolAllowed(skillCode, TOOL_READ_SKILL)) {
            return "Skill 读取失败：当前 Skill 不允许调用 readSkill";
        }
        try {
            return this.skillRegistry.getSkillBody(skillCode);
        } catch (Exception ex) {
            return "Skill 读取失败：" + ex.getMessage();
        }
    }

    /**
     * 读取 Skill 资源。
     *
     * @param skillCode Skill 编码
     * @param resourcePath 资源相对路径
     * @return 资源内容
     */
    @Tool("读取已注册 skill 的资源文件内容")
    public String readSkillResource(String skillCode, String resourcePath) {
        if (!StringUtils.hasText(skillCode)) {
            return "Skill 资源读取失败：skillCode不能为空";
        }
        if (!StringUtils.hasText(resourcePath)) {
            return "Skill 资源读取失败：resourcePath不能为空";
        }
        if (!this.skillToolPolicy.isToolAllowed(skillCode, TOOL_READ_SKILL_RESOURCE)) {
            return "Skill 资源读取失败：当前 Skill 不允许调用 readSkillResource";
        }
        try {
            Optional<SkillResource> resource = this.skillRegistry.getResource(skillCode, resourcePath);
            if (resource.isEmpty()) {
                return "Skill 资源读取失败：未找到资源 " + resourcePath;
            }
            SkillResource skillResource = resource.get();
            return skillResource.getContent() == null ? "" : skillResource.getContent();
        } catch (Exception ex) {
            return "Skill 资源读取失败：" + ex.getMessage();
        }
    }

    /**
     * 构建 LangChain4j 工具列表。
     *
     * @param activation 激活状态
     * @return 工具 Map
     */
    public Map<ToolSpecification, ToolExecutor> buildToolMap(SkillActivation activation) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        if (activation == null || activation.getLoadedSkillCodes() == null || activation.getLoadedSkillCodes().isEmpty()) {
            return tools;
        }
        tools.put(buildReadSkillSpec(), buildReadSkillExecutor());
        if (activation.getAllowedTools() != null && activation.getAllowedTools().stream().anyMatch(this::isReadSkillResourceAllowed)) {
            tools.put(buildReadSkillResourceSpec(), buildReadSkillResourceExecutor());
        }
        return tools;
    }

    private boolean isReadSkillResourceAllowed(String toolName) {
        return TOOL_READ_SKILL_RESOURCE.equalsIgnoreCase(oConvertUtils.getString(toolName));
    }

    private ToolSpecification buildReadSkillSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("skillCode", "Skill 编码")
                .required("skillCode")
                .build();
        return ToolSpecification.builder()
                .name(TOOL_READ_SKILL)
                .description("读取已注册 skill 的完整 SKILL.md 正文")
                .parameters(schema)
                .build();
    }

    private ToolSpecification buildReadSkillResourceSpec() {
        JsonObjectSchema schema = JsonObjectSchema.builder()
                .addStringProperty("skillCode", "Skill 编码")
                .addStringProperty("resourcePath", "Skill 资源相对路径")
                .required("skillCode", "resourcePath")
                .build();
        return ToolSpecification.builder()
                .name(TOOL_READ_SKILL_RESOURCE)
                .description("读取已注册 skill 的资源文件内容")
                .parameters(schema)
                .build();
    }

    private ToolExecutor buildReadSkillExecutor() {
        return (toolExecutionRequest, memoryId) -> {
            JSONObject args = parseArgs(toolExecutionRequest == null ? null : toolExecutionRequest.arguments());
            String skillCode = args == null ? null : args.getString("skillCode");
            return readSkill(skillCode);
        };
    }

    private ToolExecutor buildReadSkillResourceExecutor() {
        return (toolExecutionRequest, memoryId) -> {
            JSONObject args = parseArgs(toolExecutionRequest == null ? null : toolExecutionRequest.arguments());
            String skillCode = args == null ? null : args.getString("skillCode");
            String resourcePath = args == null ? null : args.getString("resourcePath");
            return readSkillResource(skillCode, resourcePath);
        };
    }

    private JSONObject parseArgs(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(arguments);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }
}
