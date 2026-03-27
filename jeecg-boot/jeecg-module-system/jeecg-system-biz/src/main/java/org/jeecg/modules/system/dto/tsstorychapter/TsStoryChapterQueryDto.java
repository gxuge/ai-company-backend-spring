package org.jeecg.modules.system.dto.tsstorychapter;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
@Data
public class TsStoryChapterQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    @NotNull(message = "storyId不能为空")
    private Long storyId;
    private String keyword;
    private Integer status;
}
