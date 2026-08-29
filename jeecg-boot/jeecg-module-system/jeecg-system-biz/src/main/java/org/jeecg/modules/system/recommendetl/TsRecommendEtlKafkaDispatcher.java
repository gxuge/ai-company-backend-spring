package org.jeecg.modules.system.recommendetl;

import org.jeecg.modules.system.config.TsRecommendEtlConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Kafka 推荐 ETL 分发器。 */
@Component
@ConditionalOnProperty(
        prefix = "jeecg.recommend-etl",
        name = "dispatch-mode",
        havingValue = "kafka")
public class TsRecommendEtlKafkaDispatcher
        implements TsRecommendEtlExecutionDispatcher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TsRecommendEtlConfig config;

    /** 注入 Kafka 客户端和 ETL 配置。 */
    public TsRecommendEtlKafkaDispatcher(
            KafkaTemplate<String, String> kafkaTemplate,
            TsRecommendEtlConfig config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }

    /** {@inheritDoc} */
    @Override
    public void dispatch(Long executionId) {
        try {
            kafkaTemplate.send(
                            config.getKafkaTopic(),
                            String.valueOf(executionId),
                            String.valueOf(executionId))
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka 分发 ETL 任务失败", exception);
        }
    }
}
