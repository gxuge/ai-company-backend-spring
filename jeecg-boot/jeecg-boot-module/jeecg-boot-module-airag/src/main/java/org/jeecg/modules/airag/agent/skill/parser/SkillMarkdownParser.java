package org.jeecg.modules.airag.agent.skill.parser;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.skill.model.SkillDefinition;
import org.jeecg.modules.airag.agent.skill.model.SkillResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析 Skill Markdown 文件。
 */
@Component
public class SkillMarkdownParser {
    private static final String FRONT_MATTER_DELIMITER = "---";

    /**
     * 解析完整 Skill 文本。
     *
     * @param fileName 文件名
     * @param markdown markdown 内容
     * @return Skill 定义
     */
    public SkillDefinition parse(String fileName, String markdown) {
        String safeMarkdown = markdown == null ? "" : markdown;
        String normalized = safeMarkdown.replace("\r\n", "\n");
        FrontMatterSplit split = splitFrontMatter(normalized);
        Map<String, Object> frontMatter = parseFrontMatter(split.frontMatter());

        SkillDefinition definition = new SkillDefinition();
        definition.setCode(firstText(frontMatter.get("code")));
        definition.setName(firstText(frontMatter.get("name")));
        definition.setDescription(firstText(frontMatter.get("description")));
        definition.setDomain(firstText(frontMatter.get("domain")));
        definition.setVersion(firstText(frontMatter.get("version")));
        definition.setAllowedTools(parseStringList(frontMatter.get("allowed_tools"), frontMatter.get("allowedTools")));
        definition.setRequiredInputs(parseStringList(frontMatter.get("required_inputs"), frontMatter.get("requiredInputs")));
        definition.setOptionalInputs(parseStringList(frontMatter.get("optional_inputs"), frontMatter.get("optionalInputs")));
        definition.setOutputs(parseStringList(frontMatter.get("outputs")));
        definition.setClarifyInputs(parseStringList(frontMatter.get("clarify_inputs"), frontMatter.get("clarifyInputs")));
        definition.setMetadata(parseMetadata(frontMatter));
        definition.setContent(split.body());
        definition.setResources(parseResources(frontMatter));
        if (!StringUtils.hasText(definition.getName()) && StringUtils.hasText(fileName)) {
            definition.setName(fileName);
        }
        return definition;
    }

    private Map<String, Object> parseFrontMatter(String frontMatterText) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(frontMatterText)) {
            return result;
        }
        String[] lines = frontMatterText.split("\n");
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            if (!line.startsWith(" ") && !line.startsWith("\t")) {
                if (currentKey != null) {
                    result.put(currentKey, currentValue.toString().trim());
                }
                currentKey = null;
                currentValue.setLength(0);
                int colonIndex = line.indexOf(':');
                if (colonIndex < 0) {
                    continue;
                }
                currentKey = normalizeKey(line.substring(0, colonIndex));
                String value = line.substring(colonIndex + 1).trim();
                if (StringUtils.hasText(value)) {
                    result.put(currentKey, stripQuotes(value));
                    currentKey = null;
                }
                continue;
            }
            if (currentKey != null) {
                currentValue.append(line).append('\n');
            }
        }
        if (currentKey != null) {
            result.put(currentKey, currentValue.toString().trim());
        }
        return result;
    }

    private Map<String, Object> parseMetadata(Map<String, Object> frontMatter) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (frontMatter == null || frontMatter.isEmpty()) {
            return metadata;
        }
        for (Map.Entry<String, Object> entry : frontMatter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if ("code".equalsIgnoreCase(key)
                    || "name".equalsIgnoreCase(key)
                    || "description".equalsIgnoreCase(key)
                    || "domain".equalsIgnoreCase(key)
                    || "version".equalsIgnoreCase(key)
                    || "allowed_tools".equalsIgnoreCase(key)
                    || "allowedTools".equalsIgnoreCase(key)
                    || "required_inputs".equalsIgnoreCase(key)
                    || "requiredInputs".equalsIgnoreCase(key)
                    || "optional_inputs".equalsIgnoreCase(key)
                    || "optionalInputs".equalsIgnoreCase(key)
                    || "outputs".equalsIgnoreCase(key)
                    || "clarify_inputs".equalsIgnoreCase(key)
                    || "clarifyInputs".equalsIgnoreCase(key)) {
                continue;
            }
            if ("metadata".equalsIgnoreCase(key) && value != null) {
                if (value instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> metaEntry : map.entrySet()) {
                        if (metaEntry.getKey() != null) {
                            metadata.put(String.valueOf(metaEntry.getKey()), metaEntry.getValue());
                        }
                    }
                    continue;
                }
                String text = String.valueOf(value).trim();
                if (text.startsWith("{") && text.endsWith("}")) {
                    try {
                        JSONObject object = JSON.parseObject(text);
                        if (object != null) {
                            metadata.putAll(object);
                        }
                        continue;
                    } catch (Exception ignore) {
                        // fallback to raw text
                    }
                }
                metadata.put("metadata", text);
                continue;
            }
            metadata.put(key, value);
        }
        return metadata;
    }

    private List<SkillResource> parseResources(Map<String, Object> frontMatter) {
        List<SkillResource> resources = new ArrayList<>();
        Object raw = frontMatter == null ? null : frontMatter.get("resources");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item == null) {
                    continue;
                }
                String path = String.valueOf(item).trim();
                if (!StringUtils.hasText(path)) {
                    continue;
                }
                resources.add(new SkillResource("file", path, path, null));
            }
        } else if (raw != null) {
            String text = String.valueOf(raw).trim();
            if (StringUtils.hasText(text)) {
                resources.add(new SkillResource("file", text, text, null));
            }
        }
        return resources;
    }

    private List<String> parseStringList(Object... values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    addListItem(result, item);
                }
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (text.startsWith("[") && text.endsWith("]")) {
                text = text.substring(1, text.length() - 1);
            }
            for (String part : text.split("[,;|]")) {
                addListItem(result, part);
            }
        }
        return result;
    }

    private void addListItem(List<String> result, Object item) {
        if (item == null) {
            return;
        }
        String text = String.valueOf(item).trim();
        if (StringUtils.hasText(text)) {
            result.add(text);
        }
    }

    private FrontMatterSplit splitFrontMatter(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return new FrontMatterSplit("", "");
        }
        String normalized = markdown.trim();
        if (!normalized.startsWith(FRONT_MATTER_DELIMITER)) {
            return new FrontMatterSplit("", markdown);
        }
        int endIndex = normalized.indexOf("\n" + FRONT_MATTER_DELIMITER + "\n", FRONT_MATTER_DELIMITER.length());
        if (endIndex < 0) {
            int fallback = normalized.indexOf("\n" + FRONT_MATTER_DELIMITER);
            if (fallback < 0) {
                return new FrontMatterSplit("", markdown);
            }
            endIndex = fallback;
        }
        String frontMatter = normalized.substring(FRONT_MATTER_DELIMITER.length(), endIndex).trim();
        int bodyStart = normalized.indexOf('\n', endIndex + FRONT_MATTER_DELIMITER.length() + 1);
        if (bodyStart < 0) {
            bodyStart = endIndex + FRONT_MATTER_DELIMITER.length() + 1;
        }
        String body = normalized.substring(Math.min(bodyStart, normalized.length())).trim();
        if (body.startsWith(FRONT_MATTER_DELIMITER)) {
            body = body.substring(FRONT_MATTER_DELIMITER.length()).trim();
        }
        return new FrontMatterSplit(frontMatter, body);
    }

    private String normalizeKey(String key) {
        return key == null ? null : key.trim();
    }

    private String firstText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? stripQuotes(text) : null;
    }

    private String stripQuotes(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String text = value.trim();
        if ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    /**
     * 前后提取结果。
     */
    private record FrontMatterSplit(String frontMatter, String body) {
    }
}
