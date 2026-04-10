package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

@Data
public class TsStoryOneClickSettingGenerateVo {
    private String title;
    private String storyIntro;
    private String storyMode;
    private String storySetting;
    private String storyBackground;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
}
