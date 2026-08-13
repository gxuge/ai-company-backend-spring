package org.jeecg.modules.system.dto.tsuserbrowsehistory;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户浏览记录分页查询参数。
 */
@Data
public class TsUserBrowseHistoryQueryDto {

    /** 页码，默认 1。 */
    private Integer pageNo = 1;

    /** 每页数量，默认 10，最大 100。 */
    private Integer pageSize = 10;

    /** 资源类型：role 角色，story 故事；为空时查询全部。 */
    @Pattern(regexp = "^(role|story)$", message = "资源类型仅支持role或story")
    private String resourceType;

    /** 角色名称或故事标题关键字。 */
    @Size(max = 100, message = "搜索关键字长度不能超过100个字符")
    private String keyword;
}
