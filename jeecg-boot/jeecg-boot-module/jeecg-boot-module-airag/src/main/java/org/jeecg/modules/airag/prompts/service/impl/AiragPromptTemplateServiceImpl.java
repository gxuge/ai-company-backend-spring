package org.jeecg.modules.airag.prompts.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.AssertUtils;
import org.jeecg.modules.airag.prompts.entity.AiragPrompts;
import org.jeecg.modules.airag.prompts.mapper.AiragPromptsMapper;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.jeecg.modules.airag.prompts.vo.AiragPromptTemplateVo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AIRAG Prompt 模板服务
 *
 * @author chenrui
 * @date 2026/3/31
 */
@Slf4j
@Service
public class AiragPromptTemplateServiceImpl implements IAiragPromptTemplateService {

    private static final String RESOURCE_GLOB = "classpath*:prompts/**/*.txt";
    private static final String TEMPLATE_KEY_SPLIT = "::";
    private static final String CHAT_RUNTIME_CODE = "chat_runtime";
    private static final String SECTION_OUTPUT_FIELD_NOTES = "output_field_notes";
    private static final String SECTION_TOOL_SCHEMA = "tool_schema";
    private static final String SECTION_OUTPUT_SCHEMA_HINT = "output_schema_hint";
    private static final String SECTION_META = "meta";
    private static final String META_OUTPUT_MODE = "output_mode";
    private static final String META_OUTPUT_MODE_TOOL_CALL = "tool_call";

    private static final List<String> BASE_REQUIRED_SECTIONS =
            Arrays.asList("meta", "developer_prompt", "user_prompt_template");

    private static final Pattern TEMPLATE_BEGIN_PATTERN = Pattern.compile("^TEMPLATE_BEGIN::([\\w-]+)::([\\w.-]+)$");
    private static final Pattern TEMPLATE_END_PATTERN = Pattern.compile("^TEMPLATE_END::([\\w-]+)::([\\w.-]+)$");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^SECTION::([\\w-]+)$");

    private final ResourceLoader resourceLoader;
    private final AiragPromptsMapper airagPromptsMapper;

    private volatile Map<String, AiragPromptTemplateVo> templateCache = Collections.emptyMap();

    public AiragPromptTemplateServiceImpl(ResourceLoader resourceLoader,
                                          AiragPromptsMapper airagPromptsMapper) {
        this.resourceLoader = resourceLoader;
        this.airagPromptsMapper = airagPromptsMapper;
    }

    @Override
    public Map<String, AiragPromptTemplateVo> listClasspathTemplates() {
        return templateCache;
    }

    /**
     * 启动初始化（当前不做 classpath 扫描）
     */
    @PostConstruct
    public void init() {
        // no-op: templates are loaded on demand
    }

    /**
     * 按模板编码和版本查询模板
     */
    @Override
    public AiragPromptTemplateVo getTemplate(String code, String version) {
        AssertUtils.assertNotEmpty("模板编码不能为空", code);
        AssertUtils.assertNotEmpty("模板版本不能为空", version);
        AiragPromptTemplateVo dbTemplate = loadTemplateFromDb(code, version);
        if (dbTemplate != null) {
            return dbTemplate;
        }
        String key = buildTemplateKey(code, version);
        AiragPromptTemplateVo template = templateCache.get(key);
        if (template == null) {
            throw new JeecgBootBizTipException("未找到模板：" + key);
        }
        return template;
    }

    /**
     * 渲染指定 section（仅替换 {{key}}）
     */
    @Override
    public String renderSection(String code, String version, String sectionName, Map<String, String> variables) {
        AssertUtils.assertNotEmpty("sectionName不能为空", sectionName);
        AiragPromptTemplateVo template = getTemplate(code, version);
        String sectionValue = template.getSections().get(sectionName);
        if (!template.getSections().containsKey(sectionName)) {
            throw new JeecgBootBizTipException("模板中未找到 section: " + sectionName);
        }
        if (!StringUtils.hasText(sectionValue) || CollectionUtils.isEmpty(variables)) {
            return sectionValue;
        }

        String rendered = sectionValue;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (!StringUtils.hasText(entry.getKey())) {
                continue;
            }
            String replacement = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("{{" + entry.getKey() + "}}", replacement);
        }
        return rendered;
    }

    /**
     * 扫描 classpath 模板文件并加载
     */
    private Map<String, AiragPromptTemplateVo> loadTemplatesFromResources() {
        ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        Resource[] resources;
        try {
            resources = resolver.getResources(RESOURCE_GLOB);
        } catch (IOException e) {
            throw new IllegalStateException("扫描 prompt 模板失败: " + RESOURCE_GLOB, e);
        }

        if (resources == null || resources.length == 0) {
            throw new IllegalStateException("未找到任何 prompt 模板文件: " + RESOURCE_GLOB);
        }

        Map<String, AiragPromptTemplateVo> mergedTemplates = new LinkedHashMap<>();
        int parsedFileCount = 0;
        for (Resource resource : resources) {
            String content = readResourceContent(resource);
            Map<String, AiragPromptTemplateVo> parsedFromOneFile = parseTemplates(content, resource.getDescription());
            if (parsedFromOneFile.isEmpty()) {
                continue;
            }
            parsedFileCount++;
            for (Map.Entry<String, AiragPromptTemplateVo> entry : parsedFromOneFile.entrySet()) {
                if (mergedTemplates.containsKey(entry.getKey())) {
                    AiragPromptTemplateVo existing = mergedTemplates.get(entry.getKey());
                    if (existing.getSections().equals(entry.getValue().getSections())) {
                        log.warn("检测到重复模板且内容一致，忽略后续定义，templateKey={}, source={}",
                                entry.getKey(), resource.getDescription());
                        continue;
                    }
                    throw new IllegalStateException("模板编码重复且内容不一致：" + entry.getKey() + "，请检查文件：" + resource.getDescription());
                }
                mergedTemplates.put(entry.getKey(), entry.getValue());
            }
        }

        if (mergedTemplates.isEmpty()) {
            throw new IllegalStateException("模板文件中未解析到任何 TEMPLATE_BEGIN/TEMPLATE_END 区块");
        }
        log.info("AIRAG prompt 模板扫描完成，resourceCount={}, parsedFileCount={}", resources.length, parsedFileCount);
        return mergedTemplates;
    }

    /**
     * 读取单个资源内容（UTF-8）
     */
    private String readResourceContent(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 prompt 模板失败: " + resource.getDescription(), e);
        }
    }

    /**
     * 解析模板文本为结构化 Map
     */
    private Map<String, AiragPromptTemplateVo> parseTemplates(String content, String sourceName) {
        Map<String, AiragPromptTemplateVo> parsed = new LinkedHashMap<>();
        String[] lines = content.split("\\R", -1);

        String currentCode = null;
        String currentVersion = null;
        Map<String, String> currentSections = null;
        String currentSection = null;
        StringBuilder currentSectionContent = null;

        for (String line : lines) {
            Matcher beginMatcher = TEMPLATE_BEGIN_PATTERN.matcher(line);
            if (beginMatcher.matches()) {
                if (currentCode != null) {
                    throw new IllegalStateException("模板格式错误：存在未结束模板 " + buildTemplateKey(currentCode, currentVersion)
                            + "，source=" + sourceName);
                }
                currentCode = beginMatcher.group(1);
                currentVersion = beginMatcher.group(2);
                currentSections = new LinkedHashMap<>();
                currentSection = null;
                currentSectionContent = null;
                continue;
            }

            Matcher endMatcher = TEMPLATE_END_PATTERN.matcher(line);
            if (endMatcher.matches()) {
                if (currentCode == null || currentSections == null) {
                    throw new IllegalStateException("模板格式错误：出现孤立 TEMPLATE_END " + line + "，source=" + sourceName);
                }
                if (!currentCode.equals(endMatcher.group(1)) || !currentVersion.equals(endMatcher.group(2))) {
                    throw new IllegalStateException("模板格式错误：TEMPLATE_BEGIN 与 TEMPLATE_END 不匹配，begin="
                            + buildTemplateKey(currentCode, currentVersion) + ", end=" + endMatcher.group(1) + TEMPLATE_KEY_SPLIT + endMatcher.group(2)
                            + "，source=" + sourceName);
                }
                saveSection(currentSections, currentSection, currentSectionContent);

                AiragPromptTemplateVo templateVo = new AiragPromptTemplateVo();
                templateVo.setCode(currentCode);
                templateVo.setVersion(currentVersion);
                templateVo.setSections(Collections.unmodifiableMap(new LinkedHashMap<>(currentSections)));
                parsed.put(buildTemplateKey(currentCode, currentVersion), templateVo);

                currentCode = null;
                currentVersion = null;
                currentSections = null;
                currentSection = null;
                currentSectionContent = null;
                continue;
            }

            if (currentCode == null) {
                continue;
            }

            Matcher sectionMatcher = SECTION_PATTERN.matcher(line);
            if (sectionMatcher.matches()) {
                saveSection(currentSections, currentSection, currentSectionContent);
                currentSection = sectionMatcher.group(1);
                currentSectionContent = new StringBuilder();
                continue;
            }

            if (currentSectionContent != null) {
                currentSectionContent.append(line).append('\n');
            }
        }

        if (currentCode != null) {
            throw new IllegalStateException("模板格式错误：模板缺少结束标记 TEMPLATE_END，template="
                    + buildTemplateKey(currentCode, currentVersion) + "，source=" + sourceName);
        }
        return parsed;
    }

    /**
     * 校验每个模板的必需 section
     */
    private void validateRequiredSections(Map<String, AiragPromptTemplateVo> templates) {
        for (AiragPromptTemplateVo template : templates.values()) {
            List<String> requiredSections = new ArrayList<>(BASE_REQUIRED_SECTIONS);
            if (isToolCallTemplate(template.getSections())) {
                requiredSections.add(SECTION_TOOL_SCHEMA);
            } else {
                requiredSections.add(SECTION_OUTPUT_SCHEMA_HINT);
            }
            if (CHAT_RUNTIME_CODE.equals(template.getCode()) && !isToolCallTemplate(template.getSections())) {
                requiredSections.add(SECTION_OUTPUT_FIELD_NOTES);
            }
            for (String requiredSection : requiredSections) {
                if (!template.getSections().containsKey(requiredSection)) {
                    throw new IllegalStateException("模板缺少必需 section，template="
                            + buildTemplateKey(template.getCode(), template.getVersion()) + ", section=" + requiredSection);
                }
            }
        }
    }

    /**
     * 保存 section 内容
     */
    private void saveSection(Map<String, String> sections, String sectionName, StringBuilder sectionContent) {
        if (sectionName == null || sectionContent == null) {
            return;
        }
        sections.put(sectionName, stripTrailingLineBreak(sectionContent.toString()));
    }

    /**
     * 去除 section 末尾多余换行
     */
    private String stripTrailingLineBreak(String text) {
        int end = text.length();
        while (end > 0) {
            char c = text.charAt(end - 1);
            if (c == '\n' || c == '\r') {
                end--;
            } else {
                break;
            }
        }
        return text.substring(0, end);
    }

    /**
     * 生成模板缓存 key
     */
    private AiragPromptTemplateVo loadTemplateFromDb(String code, String version) {
        AiragPrompts prompts = airagPromptsMapper.selectOne(
                new LambdaQueryWrapper<AiragPrompts>()
                        .eq(AiragPrompts::getPromptKey, code)
                        .eq(AiragPrompts::getVersion, version)
                        .orderByDesc(AiragPrompts::getUpdateTime)
                        .orderByDesc(AiragPrompts::getCreateTime)
                        .last("limit 1"),
                false
        );
        if (prompts == null) {
            return null;
        }
        AiragPromptTemplateVo fullTemplate = parseTemplateFromDbContent(prompts, code, version);
        if (fullTemplate != null) {
            return fullTemplate;
        }
        Map<String, String> sections = buildSectionsFromDb(prompts);
        if (!isValidSections(code, sections)) {
            String key = buildTemplateKey(code, version);
            throw new JeecgBootBizTipException("提示词配置不完整：" + key + "，请先在提示词管理中补齐必填段落");
        }
        AiragPromptTemplateVo templateVo = new AiragPromptTemplateVo();
        templateVo.setCode(code);
        templateVo.setVersion(version);
        templateVo.setSections(Collections.unmodifiableMap(sections));
        return templateVo;
    }

    private AiragPromptTemplateVo parseTemplateFromDbContent(AiragPrompts prompts, String code, String version) {
        if (!StringUtils.hasText(prompts.getContent())) {
            return null;
        }
        String templateText = prompts.getContent();
        if (!templateText.contains("TEMPLATE_BEGIN::")) {
            return null;
        }
        String key = buildTemplateKey(code, version);
        try {
            Map<String, AiragPromptTemplateVo> parsed = parseTemplates(templateText, "db:" + prompts.getId());
            AiragPromptTemplateVo templateVo = parsed.get(key);
            if (templateVo == null) {
                throw new JeecgBootBizTipException("description 中的模板编码/版本不匹配: " + key);
            }
            if (!isValidSections(code, templateVo.getSections())) {
                throw new JeecgBootBizTipException("description 中的模板缺少必填 SECTION: " + key);
            }
            return templateVo;
        } catch (JeecgBootBizTipException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JeecgBootBizTipException("description 中的模板格式错误: " + key);
        }
    }

    private Map<String, String> buildSectionsFromDb(AiragPrompts prompts) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("meta", "");
        sections.put("developer_prompt", "");
        sections.put("user_prompt_template", prompts.getContent());
        sections.put(SECTION_OUTPUT_SCHEMA_HINT, "");
        sections.put(SECTION_TOOL_SCHEMA, "");
        sections.put("output_field_notes", "");
        if (!StringUtils.hasText(prompts.getModelParam())) {
            return sections;
        }
        try {
            JSONObject modelParam = JSONObject.parseObject(prompts.getModelParam());
            putIfPresent(modelParam, sections, "meta");
            putIfPresent(modelParam, sections, "developer_prompt");
            putIfPresent(modelParam, sections, SECTION_OUTPUT_SCHEMA_HINT);
            putIfPresent(modelParam, sections, SECTION_TOOL_SCHEMA);
            putIfPresent(modelParam, sections, "output_field_notes");
            putIfPresent(modelParam, sections, "user_prompt_template");
        } catch (Exception ex) {
            log.warn("解析 airag_prompts.model_param 失败，将使用默认段落，promptId={}", prompts.getId(), ex);
        }
        return sections;
    }

    private void putIfPresent(JSONObject source, Map<String, String> target, String key) {
        String value = source.getString(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private boolean isValidSections(String code, Map<String, String> sections) {
        for (String requiredSection : BASE_REQUIRED_SECTIONS) {
            if (!StringUtils.hasText(sections.get(requiredSection))) {
                return false;
            }
        }
        if (isToolCallTemplate(sections)) {
            return StringUtils.hasText(sections.get(SECTION_TOOL_SCHEMA));
        }
        if (!StringUtils.hasText(sections.get(SECTION_OUTPUT_SCHEMA_HINT))) {
            return false;
        }
        if (CHAT_RUNTIME_CODE.equals(code) && !StringUtils.hasText(sections.get(SECTION_OUTPUT_FIELD_NOTES))) {
            return false;
        }
        return true;
    }

    private boolean isToolCallTemplate(Map<String, String> sections) {
        if (sections == null) {
            return false;
        }
        String meta = sections.get(SECTION_META);
        if (!StringUtils.hasText(meta)) {
            return false;
        }
        String[] lines = meta.split("\\R");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            int eqIndex = trimmed.indexOf('=');
            if (eqIndex <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eqIndex).trim();
            String value = trimmed.substring(eqIndex + 1).trim();
            if (META_OUTPUT_MODE.equalsIgnoreCase(key)) {
                return META_OUTPUT_MODE_TOOL_CALL.equalsIgnoreCase(value);
            }
        }
        return false;
    }

    private String buildTemplateKey(String code, String version) {
        return code + TEMPLATE_KEY_SPLIT + version;
    }
}


