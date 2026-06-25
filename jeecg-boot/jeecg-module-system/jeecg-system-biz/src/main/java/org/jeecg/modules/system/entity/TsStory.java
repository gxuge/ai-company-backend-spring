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
@TableName("ts_story_info")
public class TsStory implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("story_code")
    private String storyCode;
    @TableField("user_id")
    private String userId;
    private String title;
    @TableField("story_intro")
    private String storyIntro;
    @TableField("story_mode")
    private String storyMode;
    @TableField("site_setting")
    private String siteSetting;
    @TableField("story_background")
    private String storyBackground;
    @TableField("cover_url")
    private String coverUrl;
    @TableField("scene_image_url")
    private String sceneImageUrl;
    @TableField("scene_id")
    private Long sceneId;
    @TableField("scene_name_snapshot")
    private String sceneNameSnapshot;
    private Integer status;
    @TableField("is_public")
    private Integer isPublic;
    @TableField("is_ai_story_setting")
    private Integer isAiStorySetting;
    @TableField("is_ai_character")
    private Integer isAiCharacter;
    @TableField("is_ai_outline")
    private Integer isAiOutline;
    @TableField("plot_outline")
    private String plotOutline;
    @TableField("created_by")
    private String createdBy;
    @TableField("created_name")
    private String createdName;
    @TableField("updated_by")
    private String updatedBy;
    @TableField("updated_name")
    private String updatedName;
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 兼容旧代码中的 storySetting 访问，当前统一映射到 siteSetting。
     */
    public String getStorySetting() {
        return this.siteSetting;
    }

    /**
     * 兼容旧代码中的 storySetting 写入，当前统一映射到 siteSetting。
     */
    public void setStorySetting(String storySetting) {
        this.siteSetting = storySetting;
    }
}
