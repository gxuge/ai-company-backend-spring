package org.jeecg.modules.system.dto.tsrole;

import lombok.Data;
@Data
public class TsRoleQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String keyword;
    private String gender;
    private Integer status;
    private Integer isPublic;
}