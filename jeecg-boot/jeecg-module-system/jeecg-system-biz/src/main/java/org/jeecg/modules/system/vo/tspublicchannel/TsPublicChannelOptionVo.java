package org.jeecg.modules.system.vo.tspublicchannel;

import lombok.Data;

/**
 * 公开渠道下拉选项。
 */
@Data
public class TsPublicChannelOptionVo {
    private String label;
    private String value;
    private String imageUrl;
    private String targetType;
}
