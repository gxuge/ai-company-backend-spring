package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布反馈附件参数。
 */
@Data
public class TsFeedbackAttachmentDto {

    /** 已上传文件地址。 */
    @NotBlank(message = "附件地址不能为空")
    @Size(max = 1000, message = "附件地址长度不能超过1000个字符")
    private String fileUrl;

    /** 附件类型：image 图片，screenshot 截图，log 日志文件。 */
    @NotBlank(message = "附件类型不能为空")
    @Pattern(regexp = "^(image|screenshot|log)$", message = "附件类型仅支持image、screenshot或log")
    private String fileType;
}
