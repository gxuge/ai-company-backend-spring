package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
@Data
public class TsStoryQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
    private Integer isPublic;
    private String storyMode;
}