package org.jeecg.modules.system.dto.tscontenttag;

import lombok.Data;

import java.math.BigDecimal;

/** AI 生成或客户端回传的候选内容标签。 */
@Data
public class TsContentTagCandidateDto {
    /** 标签类型编码。 */
    private String typeCode;
    /** 固定标签名称。 */
    private String name;
    /** 内容与标签的匹配分数，范围 0 到 1。 */
    private BigDecimal score;
}
