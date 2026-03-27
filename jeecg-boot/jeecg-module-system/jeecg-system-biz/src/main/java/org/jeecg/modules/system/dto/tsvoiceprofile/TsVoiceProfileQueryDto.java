package org.jeecg.modules.system.dto.tsvoiceprofile;

import lombok.Data;
@Data
public class TsVoiceProfileQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String gender;
    private String ageGroup;
    private Integer status;
}
