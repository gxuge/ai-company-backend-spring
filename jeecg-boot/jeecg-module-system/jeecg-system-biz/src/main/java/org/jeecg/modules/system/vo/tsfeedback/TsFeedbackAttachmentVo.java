package org.jeecg.modules.system.vo.tsfeedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 反馈附件展示对象。
 */
@Data
public class TsFeedbackAttachmentVo {

    /** 附件 ID。 */
    private Long id;

    /** 文件地址。 */
    private String fileUrl;

    /** 文件类型：image、screenshot、log。 */
    private String fileType;

    /** 创建时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
