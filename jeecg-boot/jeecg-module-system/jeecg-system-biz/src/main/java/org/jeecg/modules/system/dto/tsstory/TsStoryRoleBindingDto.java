package org.jeecg.modules.system.dto.tsstory;

import lombok.Data;
@Data
public class TsStoryRoleBindingDto {
    private Long roleId;
    private String roleType;
    private Integer sortNo;
    private Integer isRequired;
    private String joinSource;
}