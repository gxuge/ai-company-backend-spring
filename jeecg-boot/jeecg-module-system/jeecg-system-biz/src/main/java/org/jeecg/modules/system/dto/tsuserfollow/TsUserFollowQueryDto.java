package org.jeecg.modules.system.dto.tsuserfollow;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用户关注分页查询参数。 */
@Data
public class TsUserFollowQueryDto {

    /** 页码，默认 1。 */
    private Integer pageNo = 1;

    /** 每页数量，默认 10，最大 100。 */
    private Integer pageSize = 10;

    /** 用户账号或名称关键字。 */
    @Size(max = 100, message = "搜索关键字长度不能超过100个字符")
    private String keyword;
}
