package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布反馈参数。
 */
@Data
public class TsFeedbackCreateDto {

    /** 反馈类型：feature、bug、experience。 */
    @NotBlank(message = "反馈类型不能为空")
    @Pattern(regexp = "^(feature|bug|experience)$", message = "反馈类型不正确")
    private String type;

    /** 反馈标题。 */
    @NotBlank(message = "反馈标题不能为空")
    @Size(max = 100, message = "反馈标题长度不能超过100个字符")
    private String title;

    /** 反馈内容。 */
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 10000, message = "反馈内容长度不能超过10000个字符")
    private String content;

    /** 附件列表，最多 9 个。 */
    @Valid
    @Size(max = 9, message = "反馈附件最多上传9个")
    private List<TsFeedbackAttachmentDto> attachments;
}
