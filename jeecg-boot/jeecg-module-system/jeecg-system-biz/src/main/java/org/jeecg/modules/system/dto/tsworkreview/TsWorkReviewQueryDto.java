package org.jeecg.modules.system.dto.tsworkreview;

import lombok.Data;

@Data
public class TsWorkReviewQueryDto {
    private String workType;
    private String status;
    private String ownerUserId;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
