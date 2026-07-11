package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

@Data
public class TsRoleGenerateImagePromptByTemplateVo {
    /**
     * 最终采用的风格。
     */
    private String styleUsed;

    /**
     * 可直接用于图生图/文生图的视觉提示词。
     */
    private String visualPrompt;

    /**
     * 负面提示词。
     */
    private String negativePrompt;

    /**
     * 使用的 prompt 编码。
     */
    private String promptCode;

    /**
     * 使用的 prompt 版本。
     */
    private String promptVersion;

    /**
     * 模板渲染后的完整 prompt。
     */
    private String renderedPrompt;

    /**
     * 生成链路快照 key。
     */
    private String snapshotKey;
}
