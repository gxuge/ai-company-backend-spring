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
@TableName("ts_role_info")
public class TsRole implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private String userId;

    @TableField("role_name")
    private String roleName;

    @TableField("role_subtitle")
    private String roleSubtitle;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("cover_url")
    private String coverUrl;

    private String gender;
    private String occupation;

    @TableField("intro_text")
    private String introText;

    @TableField("persona_text")
    private String personaText;

    @TableField("background_story")
    private String backgroundStory;

    @TableField("story_text")
    private String storyText;

    @TableField("dialogue_preview")
    private String dialoguePreview;

    @TableField("dialogue_length")
    private String dialogueLength;

    @TableField("tone_tendency")
    private String toneTendency;

    @TableField("interaction_mode")
    private String interactionMode;

    @TableField("voice_name")
    private String voiceName;

    /**
     * 角色编码：内置角色使用固定编码（例如 SYSTEM_ASSISTANT）
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 是否内置角色：1=内置，0=普通用户角色
     */
    @TableField("is_builtin")
    private Integer isBuiltin;

    @TableField("ext_json")
    private String extJson;

    @TableField("is_public")
    private Integer isPublic;

    @TableField("basic_ai_generated")
    private Integer basicAiGenerated;

    @TableField("advanced_ai_generated")
    private Integer advancedAiGenerated;

    private Integer status;

    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
