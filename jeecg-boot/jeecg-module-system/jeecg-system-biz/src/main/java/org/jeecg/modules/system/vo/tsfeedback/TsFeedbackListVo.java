package org.jeecg.modules.system.vo.tsfeedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 反馈列表展示对象。
 */
@Data
public class TsFeedbackListVo {

    /** 反馈 ID。 */
    private Long id;

    /** 发布用户 ID。 */
    private String userId;

    /** 发布用户名称。 */
    private String userName;

    /** 发布用户头像。 */
    private String userAvatar;

    /** 反馈类型：feature、bug、experience。 */
    private String type;

    /** 反馈标题。 */
    private String title;

    /** 反馈内容。 */
    private String content;

    /** 状态：received、processing、completed。 */
    private String status;

    /** 点赞数量。 */
    private Integer likeCount;

    /** 评论及回复总数。 */
    private Integer commentCount;

    /** 当前用户是否已点赞。 */
    private Boolean liked;

    /** 创建时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
