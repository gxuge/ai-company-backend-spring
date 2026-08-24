package org.jeecg.modules.system.vo.tsbehavior;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 推荐行为采集接收结果。 */
@Data
@AllArgsConstructor
public class TsBehaviorCollectVo {
    /** 已提交到 Kafka Producer 的事件数量。 */
    private int acceptedCount;
}
