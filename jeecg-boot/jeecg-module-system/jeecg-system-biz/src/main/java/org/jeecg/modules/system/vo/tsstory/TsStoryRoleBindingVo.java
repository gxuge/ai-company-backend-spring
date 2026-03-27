package org.jeecg.modules.system.vo.tsstory;

import lombok.Data;
@Data
public class TsStoryRoleBindingVo {
    private Long roleId;
    private String roleType;
    private Integer sortNo;
    private Integer isRequired;
    private String joinSource;
}