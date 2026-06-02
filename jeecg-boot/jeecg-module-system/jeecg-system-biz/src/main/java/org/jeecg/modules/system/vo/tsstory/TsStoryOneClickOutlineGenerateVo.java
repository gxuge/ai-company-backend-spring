package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

import java.util.List;

@Data
public class TsStoryOneClickOutlineGenerateVo {
    private List<TsStoryOneClickOutlineChapterVo> chapters;
    /** 单字段优化模式下返回的剧情大纲文本 */
    private String plotOutline;
    private String promptCode;
    private String promptVersion;
    private String renderedPrompt;
    private String snapshotKey;
}
