package org.jeecg.modules.system.vo.tsrolepublic;

import lombok.Data;

/**
 * 角色公开目标下拉选项。
 */
@Data
public class TsRolePublicTargetOptionVo {
    private Long value;
    private String label;
    private String ownerUserId;
}
