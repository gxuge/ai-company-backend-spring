package org.jeecg.modules.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 推荐训练数据 ETL 配置。 */
@Data
@Component
@ConfigurationProperties(prefix = TsRecommendEtlConfig.PREFIX)
public class TsRecommendEtlConfig {
    public static final String PREFIX = "jeecg.recommend-etl";

    /** 是否启用 ETL 编排。 */
    private boolean enabled = true;
    /** 执行分发模式：local/kafka。 */
    private String dispatchMode = "local";
    /** Python 可执行文件。 */
    private String pythonExecutable = "python";
    /** 允许的脚本根目录。 */
    private String scriptRoot = "./etl/scripts";
    /** 允许的输出根目录。 */
    private String outputRoot = "./etl/output";
    /** 完整日志根目录。 */
    private String logRoot = "./etl/logs";
    /** 默认超时秒数。 */
    private int defaultTimeoutSeconds = 3600;
    /** 最大超时秒数。 */
    private int maxTimeoutSeconds = 86400;
    /** 数据库存储的最大日志字符数。 */
    private int maxLogChars = 1000000;
    /** 本地执行线程数。 */
    private int workerCoreSize = 2;
    /** 本地执行最大线程数。 */
    private int workerMaxSize = 4;
    /** 本地执行队列容量。 */
    private int workerQueueCapacity = 100;
    /** Kafka Topic。 */
    private String kafkaTopic = "ts.recommend-etl.execute.v1";
    /** Kafka 消费组。 */
    private String kafkaGroup = "ts-recommend-etl-worker-v1";
    /** Kafka 分区数。 */
    private int kafkaPartitions = 3;
    /** Kafka 副本数。 */
    private short kafkaReplicas = 1;
}
