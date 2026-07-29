package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;

/**
 * 使用已确认的角色核心字段生成完整角色请求。
 */
@Data
public class TsRoleConfirmedGenerateDto {
    /** 角色名称。 */
    private String roleName;
    /** 角色性别。 */
    private String gender;
    /** 角色职业或身份。 */
    private String occupation;
    /** 角色背景故事。 */
    private String backgroundStory;

    /**
     * 清理字符串空白。
     */
    public void normalize() {
        this.roleName = trimToNull(this.roleName);
        this.gender = trimToNull(this.gender);
        this.occupation = trimToNull(this.occupation);
        this.backgroundStory = trimToNull(this.backgroundStory);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
