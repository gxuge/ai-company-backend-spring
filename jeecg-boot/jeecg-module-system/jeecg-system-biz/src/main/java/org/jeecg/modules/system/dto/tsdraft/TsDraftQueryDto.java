package org.jeecg.modules.system.dto.tsdraft;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 统一草稿分页查询参数。
 */
@Data
public class TsDraftQueryDto {

    /** 页码，默认 1。 */
    private Integer pageNo = 1;

    /** 每页数量，默认 10，最大 100。 */
    private Integer pageSize = 10;

    /** 草稿名称关键字。 */
    @Size(max = 200, message = "草稿关键字长度不能超过200个字符")
    private String keyword;

    /** 草稿类型：role 角色，story 故事。 */
    @Pattern(regexp = "^(role|story)$", message = "草稿类型仅支持role或story")
    private String draftType;

    /** 来源正式资源 ID。 */
    @Positive(message = "来源资源ID必须为正整数")
    private Long sourceId;
}
