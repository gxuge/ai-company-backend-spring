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
@TableName("ts_voice_profile")
public class TsVoiceProfile implements Serializable {

    private static final long serialVersionUID = 1L;
    /** 主键 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 音色名称 */
    @TableField("name")
    private String name;
    /** 提供商音色 ID（用于对接 MiniMax 等 TTS 服务） */
    @TableField("provider_voice_id")
    private String providerVoiceId;
    /** 音色头像 URL */
    @TableField("avatar_url")
    private String avatarUrl;
    /** 性别标签（male/female/unknown） */
    @TableField("gender")
    private String gender;
    /** 年龄段标签（child/teen/adult/senior） */
    @TableField("age_group")
    private String ageGroup;
    /** 状态：0-禁用，1-启用 */
    @TableField("status")
    private Integer status;
    /** 排序号（越小越靠前） */
    @TableField("sort_no")
    private Integer sortNo;
    /** 创建时间 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    /** 更新时间 */
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
