package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_role_image_generate_record")
public class TsRoleImageGenerateRecord implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("role_id")
    private Long roleId;
    @TableField("source_profile_url")
    private String sourceProfileUrl;
    @TableField("user_id")
    private String userId;
    @TableField("prompt_text")
    private String promptText;
    @TableField("style_name")
    private String styleName;
    @TableField("reference_assets_json")
    private String referenceAssetsJson;
    @TableField("generate_status")
    private String generateStatus;
    @TableField("apply_status")
    private String applyStatus;
    @TableField("result_asset_id")
    private Long resultAssetId;
    @TableField("result_image_url")
    private String resultImageUrl;
    @TableField("fail_reason")
    private String failReason;
    @TableField("request_id")
    private String requestId;
    @TableField("ext_json")
    private String extJson;
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}