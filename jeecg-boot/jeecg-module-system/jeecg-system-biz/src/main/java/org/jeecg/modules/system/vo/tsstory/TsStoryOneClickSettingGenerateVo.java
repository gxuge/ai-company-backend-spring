package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

@Data
public class TsStoryOneClickSettingGenerateVo {
    private String title;
    private String storyIntro;
    private String storyMode;
    private String storySetting;
    private String storyBackground;
    /** 是否由模型成功生成（false 表示回退兜底结果） */
    private Boolean generated;
    /** 回退原因，仅 generated=false 时可能有值 */
    private String fallbackReason;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
}
