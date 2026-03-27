package org.jeecg.modules.system.dto.tsvoicetag;

import lombok.Data;
@Data
public class TsVoiceTagQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String keyword;
}
