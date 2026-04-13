package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class TsRoleGenerateTextByTemplateDto {
    /**
     * Prompt 模板编码，默认 role_ai_generate_text。
     */
    private String promptCode;

    /**
     * Prompt 版本，默认 v1。
     */
    private String promptVersion;

    /**
     * 模板变量，调用方按需传入。
     */
    private Map<String, Object> variables;

    public void normalize() {
        this.promptCode = trimToNull(this.promptCode);
        this.promptVersion = trimToNull(this.promptVersion);
        if (this.variables == null) {
            this.variables = new LinkedHashMap<>();
            return;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : this.variables.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = trimToNull(entry.getKey());
            if (key == null) {
                continue;
            }
            normalized.put(key, entry.getValue());
        }
        this.variables = normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
