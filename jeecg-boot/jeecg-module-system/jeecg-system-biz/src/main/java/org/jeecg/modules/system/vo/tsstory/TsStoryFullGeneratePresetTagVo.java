package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;

@Data
public class TsStoryFullGeneratePresetTagVo {
    private String presetTagId;
    private String tagId;
    private String typeId;
    private String tagName;
    private String promptText;
    private Integer required;
    private Integer weightOverride;
    private Integer sortOrder;
}
