package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsRoleOneClickVoiceGenerateDto {
    /** 角色ID（可选，传入时会校验归属） */
    private Long roleId;
    /** 角色名称 */
    private String roleName;
    /** 性别（male/female/unknown/random） */
    private String gender;
    /** 职业设定 */
    private String occupation;
    /** 角色背景故事 */
    private String backgroundStory;
    /** 期望音色名称关键词 */
    private String preferredVoiceName;
    /** 目标语气/氛围 */
    private String targetTone;
    /** 试听文案（可选） */
    private String previewText;

    public void normalize() {
        this.roleName = trimToNull(this.roleName);
        this.gender = normalizeGender(this.gender);
        this.occupation = trimToNull(this.occupation);
        this.backgroundStory = trimToNull(this.backgroundStory);
        this.preferredVoiceName = trimToNull(this.preferredVoiceName);
        this.targetTone = trimToNull(this.targetTone);
        this.previewText = trimToNull(this.previewText);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeGender(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if ("random".equals(lower)) {
            return null;
        }
        if ("male".equals(lower) || "female".equals(lower) || "unknown".equals(lower)) {
            return lower;
        }
        return null;
    }
}
