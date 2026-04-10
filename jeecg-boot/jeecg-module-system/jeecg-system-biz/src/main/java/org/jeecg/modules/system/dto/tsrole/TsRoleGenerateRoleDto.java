package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;

/**
 * 随机完整角色生成请求。
 */
@Data
public class TsRoleGenerateRoleDto {
    /** 场景设定（可选） */
    private String storySetting;
    /** 场所设定（可选） */
    private String storyBackground;

    /**
     * 统一清理字符串空白输入。
     */
    public void normalize() {
        this.storySetting = trimToNull(this.storySetting);
        this.storyBackground = trimToNull(this.storyBackground);
    }

    /**
     * 去除首尾空白并将空串转为 null。
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
