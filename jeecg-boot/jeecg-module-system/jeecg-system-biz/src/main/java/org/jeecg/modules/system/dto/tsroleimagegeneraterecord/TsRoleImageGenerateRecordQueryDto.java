package org.jeecg.modules.system.dto.tsroleimagegeneraterecord;

import lombok.Data;
@Data
public class TsRoleImageGenerateRecordQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Long roleId;
    private String generateStatus;
    private String applyStatus;
    private String requestId;
}