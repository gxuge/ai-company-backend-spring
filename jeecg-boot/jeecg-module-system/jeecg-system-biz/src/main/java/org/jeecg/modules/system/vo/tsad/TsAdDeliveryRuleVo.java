package org.jeecg.modules.system.vo.tsad;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 广告投放规则响应。 */
@Data
public class TsAdDeliveryRuleVo {
    /** 规则ID，默认规则为空。 */
    private Long id;
    /** 广告内容ID。 */
    private Long contentId;
    /** 平台数组。 */
    private List<String> platforms = new ArrayList<>();
    /** 受众类型。 */
    private String audienceType;
    /** 会员等级数组。 */
    private List<String> memberLevels = new ArrayList<>();
    /** 指定用户ID数组。 */
    private List<String> userIds = new ArrayList<>();
}
