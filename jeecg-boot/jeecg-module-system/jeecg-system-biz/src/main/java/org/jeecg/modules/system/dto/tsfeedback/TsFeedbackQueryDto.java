package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 反馈分页查询参数。
 */
@Data
public class TsFeedbackQueryDto {

    /** 页码，默认 1。 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = 1;

    /** 每页数量，默认 10，最大 100。 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;

    /** 反馈类型，为空时查询全部。 */
    @Pattern(regexp = "^$|^(feature|bug|experience)$", message = "反馈类型不正确")
    private String type;

    /** 反馈状态，为空时查询全部。 */
    @Pattern(regexp = "^$|^(received|processing|completed)$", message = "反馈状态不正确")
    private String status;

    /** 排序方式：latest 最新，hot 最热。 */
    @Pattern(regexp = "^$|^(latest|hot)$", message = "排序方式仅支持latest或hot")
    private String sort = "latest";

    /** 标题或内容关键字。 */
    @Size(max = 100, message = "搜索关键字长度不能超过100个字符")
    private String keyword;
}
