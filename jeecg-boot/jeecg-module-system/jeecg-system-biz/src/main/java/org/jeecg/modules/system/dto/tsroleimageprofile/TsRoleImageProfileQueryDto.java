package org.jeecg.modules.system.dto.tsroleimageprofile;

import lombok.Data;
@Data
public class TsRoleImageProfileQueryDto {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String profileName;
    private String styleName;
    private String sourceType;
    private Integer isPublic;
    private Integer status;
}