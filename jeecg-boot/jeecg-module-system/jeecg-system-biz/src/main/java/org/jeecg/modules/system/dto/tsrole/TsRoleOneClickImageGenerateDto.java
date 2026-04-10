package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class TsRoleOneClickImageGenerateDto {
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
    /** 画风名称 */
    private String styleName;
    /** 画面比例 */
    private String aspectRatio;
    /** 参考图片URL（可选） */
    private String referenceImageUrl;

    public void normalize() {
        this.roleName = trimToNull(this.roleName);
        this.gender = normalizeGender(this.gender);
        this.occupation = trimToNull(this.occupation);
        this.backgroundStory = trimToNull(this.backgroundStory);
        this.styleName = trimToNull(this.styleName);
        this.aspectRatio = trimToNull(this.aspectRatio);
        this.referenceImageUrl = trimToNull(this.referenceImageUrl);
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
