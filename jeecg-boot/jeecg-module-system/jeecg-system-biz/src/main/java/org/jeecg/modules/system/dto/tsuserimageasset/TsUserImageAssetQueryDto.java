package org.jeecg.modules.system.dto.tsuserimageasset;

import lombok.Data;
@Data
public class TsUserImageAssetQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String sourceType;
    private Integer status;
}
