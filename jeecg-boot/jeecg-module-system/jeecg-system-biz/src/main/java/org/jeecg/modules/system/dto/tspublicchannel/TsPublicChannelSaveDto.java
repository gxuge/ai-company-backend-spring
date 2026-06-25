package org.jeecg.modules.system.dto.tspublicchannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 公开渠道保存参数。
 */
@Data
public class TsPublicChannelSaveDto {
    public interface Create {}
    public interface Update {}

    /** 渠道ID。 */
    @NotNull(message = "编辑渠道时id不能为空", groups = Update.class)
    private Long id;
    /** 渠道编码。 */
    @NotBlank(message = "channelCode不能为空", groups = {Create.class, Update.class})
    private String channelCode;
    /** 渠道名称。 */
    @NotBlank(message = "channelName不能为空", groups = {Create.class, Update.class})
    private String channelName;
    /** 渠道图片。 */
    private String channelImageUrl;
    /** 目标类型：role/story/both。 */
    @NotBlank(message = "targetType不能为空", groups = {Create.class, Update.class})
    private String targetType;
    /** 状态。 */
    private String status;
    /** 排序值。 */
    private Integer sortOrder;
    /** 备注。 */
    private String remark;

    public void applyCreateDefaults() {
        if (status == null || status.trim().isEmpty()) {
            status = "enabled";
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
