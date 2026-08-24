package org.jeecg.modules.system.dto.tsad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 广告投放规则保存参数。 */
@Data
public class TsAdDeliveryRuleSaveDto {
    /** 广告内容ID。 */
    @NotNull
    private Long contentId;
    /** 平台数组：ALL/WEB/IOS/ANDROID。 */
    @NotEmpty
    private List<String> platforms;
    /** 受众类型：ALL/LOGIN/ANONYMOUS/USER_LIST。 */
    @NotBlank
    private String audienceType;
    /** 会员等级数组：ALL/FREE/PRO/ULTRA。 */
    @NotEmpty
    private List<String> memberLevels;
    /** USER_LIST受众的用户ID数组。 */
    private List<String> userIds;
}
