package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 广告位配置。 */
@Data
@Accessors(chain = true)
@TableName("ts_ad_slot")
public class TsAdSlot {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 广告位编码。 */
    private String slotCode;
    /** 广告位名称。 */
    private String slotName;
    /** 类型：BANNER/POSTER/POPUP/CAROUSEL。 */
    private String slotType;
    /** 建议宽度。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer width;
    /** 建议高度。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer height;
    /** 单次最多返回内容数。 */
    private Integer maxItems;
    /** 状态：ENABLED/DISABLED。 */
    private String status;
    /** 广告位说明。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    /** 逻辑删除：0正常，1删除。 */
    @TableLogic
    private Integer isDeleted;
    /** 创建人。 */
    private String createdBy;
    /** 更新人。 */
    private String updatedBy;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
