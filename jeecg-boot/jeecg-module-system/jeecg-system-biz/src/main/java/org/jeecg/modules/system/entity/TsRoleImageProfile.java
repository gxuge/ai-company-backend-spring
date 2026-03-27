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
@TableName("ts_role_image_profile")
public class TsRoleImageProfile implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("profile_name")
    private String profileName;
    @TableField("owner_user_id")
    private String ownerUserId;
    @TableField("prompt_text")
    private String promptText;
    @TableField("style_name")
    private String styleName;
    @TableField("selected_image_asset_id")
    private Long selectedImageAssetId;
    @TableField("selected_image_url")
    private String selectedImageUrl;
    @TableField("source_type")
    private String sourceType;
    @TableField("is_public")
    private Integer isPublic;
    @TableField("status")
    private Integer status;
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