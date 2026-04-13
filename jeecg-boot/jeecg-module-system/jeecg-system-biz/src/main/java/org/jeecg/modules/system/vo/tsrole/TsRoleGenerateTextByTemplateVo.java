package org.jeecg.modules.system.vo.tsrole;

import lombok.Data;

@Data
public class TsRoleGenerateTextByTemplateVo {
    /**
     * 模板生成后的文本，前端可直接回填输入框。
     */
    private String generatedText;

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
