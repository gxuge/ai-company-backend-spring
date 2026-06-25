package org.jeecg.modules.system.dto.tsrolepublic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色公开记录保存参数。
 */
@Data
public class TsRolePublicSaveDto {
    public interface Create {}
    public interface Update {}

    /** 公开记录ID。 */
    @NotNull(message = "编辑公开记录时id不能为空", groups = Update.class)
    private Long id;
    /** 角色ID。 */
    @NotNull(message = "roleId不能为空", groups = {Create.class, Update.class})
    private Long roleId;
    /** 所属用户ID。 */
    @NotBlank(message = "ownerUserId不能为空", groups = {Create.class, Update.class})
    private String ownerUserId;
    /** 渠道编码。 */
    @NotBlank(message = "channelCode不能为空", groups = {Create.class, Update.class})
    private String channelCode;
    /** 展示标题。 */
    private String displayTitle;
    /** 展示副标题。 */
    private String displaySubtitle;
    /** 封面图。 */
    private String coverImageUrl;
    /** 展示简介。 */
    private String introText;
    /** 排序值。 */
    private Integer sortOrder;
    /** 扩展JSON。 */
    private String extJson;

    public void applyCreateDefaults() {
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
