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
@TableName("ts_story_chapter")
public class TsStoryChapter implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("story_id")
    private Long storyId;
    @TableField("chapter_no")
    private Integer chapterNo;
    @TableField("chapter_title")
    private String chapterTitle;
    @TableField("chapter_desc")
    private String chapterDesc;
    @TableField("opening_content")
    private String openingContent;
    @TableField("opening_role_id")
    private Long openingRoleId;
    @TableField("mission_target")
    private String missionTarget;
    private Integer status;
    @TableField("is_ai_generated")
    private Integer isAiGenerated;
    @TableField("sort_no")
    private Integer sortNo;
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}