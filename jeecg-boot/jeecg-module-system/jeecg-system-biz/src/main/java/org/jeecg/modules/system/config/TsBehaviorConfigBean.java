package org.jeecg.modules.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 推荐行为采集配置。 */
@Data
@Component
@ConfigurationProperties(prefix = TsBehaviorConfigBean.PREFIX)
public class TsBehaviorConfigBean {
    public static final String PREFIX = "jeecg.behavior";

    /** Kafka 配置。 */
    private Kafka kafka = new Kafka();
    /** 单批最大事件数。 */
    private int maxBatchSize = 100;
    /** 单条扩展属性最大字节数。 */
    private int maxPropertiesBytes = 8192;
    /** Redis 实时特征有效天数。 */
    private int featureTtlDays = 30;
    /** Redis 事件去重有效天数。 */
    private int dedupTtlDays = 7;

    /** Kafka 行为链路配置。 */
    @Data
    public static class Kafka {
        /** 是否启用行为采集。 */
        private boolean enabled;
        /** 主行为 Topic。 */
        private String topic = "ts.user-behavior.v1";
        /** 死信 Topic。 */
        private String dlqTopic = "ts.user-behavior.dlq.v1";
        /** Topic 分区数量。 */
        private int partitions = 6;
        /** Topic 副本数量。 */
        private short replicas = 1;
    }
}
