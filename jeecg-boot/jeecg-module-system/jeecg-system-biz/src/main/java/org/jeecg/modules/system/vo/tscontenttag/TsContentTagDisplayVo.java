package org.jeecg.modules.system.vo.tscontenttag;

import lombok.Data;

/** 面向用户展示的内容标签。 */
@Data
public class TsContentTagDisplayVo {
    /** 标签 ID。 */
    private Long tagId;
    /** 标签类型编码。 */
    private String typeCode;
    /** 标签类型名称。 */
    private String typeName;
    /** 标签名称。 */
    private String name;
}
