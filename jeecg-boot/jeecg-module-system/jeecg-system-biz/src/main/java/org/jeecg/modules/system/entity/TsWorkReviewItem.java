package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ts_work_review_item")
public class TsWorkReviewItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("review_id")
    private Long reviewId;
    @TableField("item_type")
    private String itemType;
    @TableField("field_code")
    private String fieldCode;
    @TableField("content_text")
    private String contentText;
    @TableField("asset_url")
    private String assetUrl;
    @TableField("content_hash")
    private String contentHash;
    @TableField("created_at")
    private Date createdAt;
}
