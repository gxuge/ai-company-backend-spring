package org.jeecg.modules.system.vo.tsstorypublic;

import lombok.Data;

/**
 * 故事公开目标下拉选项。
 */
@Data
public class TsStoryPublicTargetOptionVo {
    private Long value;
    private String label;
    private String ownerUserId;
}
