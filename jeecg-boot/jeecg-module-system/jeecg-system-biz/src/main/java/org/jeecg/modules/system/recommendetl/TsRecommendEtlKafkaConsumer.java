package org.jeecg.modules.system.recommendetl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka 推荐 ETL 执行消费者。 */
@Component
@ConditionalOnProperty(
        prefix = "jeecg.recommend-etl",
        name = "dispatch-mode",
        havingValue = "kafka")
public class TsRecommendEtlKafkaConsumer {
    private final TsRecommendEtlExecutionWorker worker;

    /** 注入统一执行 Worker。 */
    public TsRecommendEtlKafkaConsumer(TsRecommendEtlExecutionWorker worker) {
        this.worker = worker;
    }

    /** 消费执行记录 ID 并启动 Python。 */
    @KafkaListener(
            topics = "${jeecg.recommend-etl.kafka-topic:ts.recommend-etl.execute.v1}",
            groupId = "${jeecg.recommend-etl.kafka-group:ts-recommend-etl-worker-v1}")
    public void consume(String executionId) {
        worker.execute(Long.valueOf(executionId));
    }
}
