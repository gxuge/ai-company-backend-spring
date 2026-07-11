package org.jeecg.modules.airag.agent.skill.registry;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.guard.SkillValidator;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.jeecg.modules.airag.agent.skill.model.SkillResource;
import org.jeecg.modules.airag.agent.skill.parser.SkillMarkdownParser;
import org.jeecg.modules.airag.agent.skill.runtime.SkillProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文件系统 Skill 注册中心。
 */
@Slf4j
@Component
public class FileSystemSkillRegistry implements SkillRegistry {
    private static final String CLASSPATH_SKILL_PATTERN = "classpath*:skills/**/SKILL.md";

    private final SkillProperties skillProperties;
    private final SkillMarkdownParser skillMarkdownParser;
    private final SkillValidator skillValidator;
    private final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private volatile Map<String, SkillRecord> skillIndex = Collections.emptyMap();

    public FileSystemSkillRegistry(SkillProperties skillProperties,
                                   SkillMarkdownParser skillMarkdownParser,
                                   SkillValidator skillValidator) {
        this.skillProperties = skillProperties;
        this.skillMarkdownParser = skillMarkdownParser;
        this.skillValidator = skillValidator;
    }

    @PostConstruct
    public void loadSkillIndex() {
        Map<String, SkillRecord> loaded = new LinkedHashMap<>();
        Path rootPath = resolveRootPath();
        boolean loadedFromFileSystem = false;
        if (rootPath != null && Files.exists(rootPath)) {
            List<Path> skillFiles = new ArrayList<>();
            try {
                Files.walk(rootPath)
                        .filter(path -> Files.isRegularFile(path)
                                && "SKILL.md".equalsIgnoreCase(path.getFileName().toString()))
                        .forEach(skillFiles::add);
                skillFiles.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
                for (Path skillFile : skillFiles) {
                    try {
                        String markdown = Files.readString(skillFile, StandardCharsets.UTF_8);
                        SkillDefinition definition = this.skillMarkdownParser.parse(skillFile.getFileName().toString(), markdown);
                        this.skillValidator.validateDefinition(definition);
                        String skillCode = definition.getCode();
                        if (!StringUtils.hasText(skillCode)) {
                            log.warn("跳过未配置 code 的 Skill 文件: {}", skillFile);
                            continue;
                        }
                        SkillDefinition indexDefinition = definition.copyWithoutContent();
                        loaded.put(skillCode, new SkillRecord(skillFile, null, markdown, indexDefinition));
                    } catch (Exception ex) {
                        log.warn("加载 Skill 失败: {}", skillFile, ex);
                    }
                }
                loadedFromFileSystem = !loaded.isEmpty();
            } catch (Exception ex) {
                log.warn("文件系统 Skill 索引加载失败，准备尝试 classpath 扫描", ex);
            }
        } else {
            log.info("Skill 根目录不存在或未配置，rootDir={}", this.skillProperties.getRootDir());
        }

        if (!loadedFromFileSystem) {
            loadFromClasspath(loaded);
        }

        this.skillIndex = loaded;
        log.info("Skill 索引加载完成，共 {} 个", loaded.size());
    }

    @Override
    public List<SkillDefinition> listSkillIndex(String domain) {
        if (this.skillIndex.isEmpty()) {
            return Collections.emptyList();
        }
        List<SkillDefinition> result = new ArrayList<>();
        for (SkillRecord record : this.skillIndex.values()) {
            if (record == null || record.definition() == null) {
                continue;
            }
            if (StringUtils.hasText(domain) && !domain.equalsIgnoreCase(oConvertUtils.getString(record.definition().getDomain()))) {
                continue;
            }
            result.add(record.definition().copyWithoutContent());
        }
        return result;
    }

    @Override
    public String getSkillBody(String skillCode) {
        SkillRecord record = this.skillIndex.get(skillCode);
        if (record == null) {
            throw new JeecgBootException("未找到Skill: " + skillCode);
        }
        if (StringUtils.hasText(record.markdown())) {
            return record.markdown();
        }
        if (record.resource() != null) {
            try {
                return new String(record.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new JeecgBootException("读取Skill失败: " + skillCode + "，" + ex.getMessage());
            }
        }
        try {
            return Files.readString(record.filePath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new JeecgBootException("读取Skill失败: " + skillCode + "，" + ex.getMessage());
        }
    }

    @Override
    public Optional<SkillResource> getResource(String skillCode, String resourcePath) {
        SkillRecord record = this.skillIndex.get(skillCode);
        if (record == null || !StringUtils.hasText(resourcePath)) {
            return Optional.empty();
        }
        if (record.filePath() != null) {
            Path skillRoot = record.filePath().getParent();
            if (skillRoot == null) {
                return Optional.empty();
            }
            Path resolved = skillRoot.resolve(resourcePath).normalize();
            if (!resolved.startsWith(skillRoot.normalize())) {
                throw new JeecgBootException("非法的资源路径: " + resourcePath);
            }
            if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
                return Optional.empty();
            }
            try {
                String content = Files.readString(resolved, StandardCharsets.UTF_8);
                String name = resolved.getFileName() == null ? resourcePath : resolved.getFileName().toString();
                String type = inferType(name);
                String relativePath = skillRoot.normalize().relativize(resolved.normalize()).toString().replace("\\", "/");
                return Optional.of(new SkillResource(type, relativePath, name, content));
            } catch (IOException ex) {
                throw new JeecgBootException("读取Skill资源失败: " + ex.getMessage());
            }
        }
        if (record.resource() != null) {
            try {
                Resource resolved = record.resource().createRelative(resourcePath);
                if (resolved == null || !resolved.exists() || !resolved.isReadable()) {
                    return Optional.empty();
                }
                String content = new String(resolved.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String name = resolved.getFilename() == null ? resourcePath : resolved.getFilename();
                String type = inferType(name);
                return Optional.of(new SkillResource(type, resourcePath, name, content));
            } catch (IOException ex) {
                throw new JeecgBootException("读取Skill资源失败: " + ex.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SkillDefinition> findSkill(String skillCode) {
        SkillRecord record = this.skillIndex.get(skillCode);
        if (record == null || record.definition() == null) {
            return Optional.empty();
        }
        return Optional.of(record.definition().copyWithoutContent());
    }

    private Path resolveRootPath() {
        String rootDir = this.skillProperties == null ? null : this.skillProperties.getRootDir();
        if (!StringUtils.hasText(rootDir)) {
            return null;
        }
        if (rootDir.startsWith("classpath:") || rootDir.startsWith("classpath*:")) {
            return null;
        }
        Path path = Paths.get(rootDir);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath().normalize();
        }
        return path;
    }

    private void loadFromClasspath(Map<String, SkillRecord> loaded) {
        try {
            String rootDir = this.skillProperties == null ? null : this.skillProperties.getRootDir();
            String pattern = buildClasspathPattern(rootDir);
            Resource[] resources = this.resourcePatternResolver.getResources(pattern);
            if (resources == null || resources.length == 0) {
                return;
            }
            for (Resource resource : resources) {
                if (resource == null || !resource.exists() || !resource.isReadable()) {
                    continue;
                }
                try {
                    String markdown = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    String filename = resource.getFilename();
                    SkillDefinition definition = this.skillMarkdownParser.parse(filename, markdown);
                    this.skillValidator.validateDefinition(definition);
                    String skillCode = definition.getCode();
                    if (!StringUtils.hasText(skillCode) || loaded.containsKey(skillCode)) {
                        continue;
                    }
                    SkillDefinition indexDefinition = definition.copyWithoutContent();
                    loaded.put(skillCode, new SkillRecord(null, resource, markdown, indexDefinition));
                } catch (Exception ex) {
                    log.warn("加载 classpath Skill 失败: {}", resource, ex);
                }
            }
        } catch (Exception ex) {
            log.warn("classpath Skill 索引加载失败", ex);
        }
    }

    private String buildClasspathPattern(String rootDir) {
        if (!StringUtils.hasText(rootDir)) {
            return CLASSPATH_SKILL_PATTERN;
        }
        String normalized = rootDir.trim();
        if (normalized.startsWith("classpath*:")) {
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized + "/**/SKILL.md";
        }
        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return "classpath*:" + normalized + "/**/SKILL.md";
        }
        return CLASSPATH_SKILL_PATTERN;
    }

    private String inferType(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "file";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".md")) {
            return "markdown";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return "yaml";
        }
        if (lower.endsWith(".txt")) {
            return "text";
        }
        return "file";
    }

    /**
     * Skill 索引记录。
     */
    private record SkillRecord(Path filePath, Resource resource, String markdown, SkillDefinition definition) {
    }
}
