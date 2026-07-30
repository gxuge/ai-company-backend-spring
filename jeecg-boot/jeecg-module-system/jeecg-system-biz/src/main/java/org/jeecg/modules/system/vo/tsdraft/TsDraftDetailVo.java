package org.jeecg.modules.system.vo.tsdraft;

import lombok.Data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一草稿详情。
 */
@Data
public class TsDraftDetailVo {

    /** 草稿主键。 */
    private Long id;

    /** 草稿类型：role 角色，story 故事。 */
    private String draftType;

    /** 草稿箱展示名称。 */
    private String draftName;

    /** 来源正式资源 ID。 */
    private Long sourceId;

    /** 页面完整状态对象。 */
    private Map<String, Object> content = new LinkedHashMap<>();

    /** 状态：1 正常，0 已删除。 */
    private Integer status;

    /** 创建时间。 */
    private Date createdAt;

    /** 更新时间。 */
    private Date updatedAt;
}
