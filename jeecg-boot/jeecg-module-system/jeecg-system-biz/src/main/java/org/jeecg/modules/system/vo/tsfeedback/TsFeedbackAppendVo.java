package org.jeecg.modules.system.vo.tsfeedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 追加反馈展示对象。
 */
@Data
public class TsFeedbackAppendVo {

    /** 追加反馈 ID。 */
    private Long id;

    /** 所属反馈 ID。 */
    private Long feedbackId;

    /** 追加用户 ID。 */
    private String userId;

    /** 追加内容。 */
    private String content;

    /** 创建时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
