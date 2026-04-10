package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.List;

@Data
public class TsStoryOneClickOutlineGenerateVo {
    private List<TsStoryOneClickOutlineChapterVo> chapters;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
}
