package org.jeecg.modules.system.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;

/** 推荐 ETL Kafka Topic 配置。 */
@Configuration
@ConditionalOnProperty(
        prefix = "jeecg.recommend-etl",
        name = "dispatch-mode",
        havingValue = "kafka")
public class TsRecommendEtlKafkaConfig {

    /** 声明推荐 ETL 执行 Topic。 */
    @Bean
    public KafkaAdmin.NewTopics tsRecommendEtlTopics(TsRecommendEtlConfig config) {
        NewTopic topic = TopicBuilder.name(config.getKafkaTopic())
                .partitions(config.getKafkaPartitions())
                .replicas(config.getKafkaReplicas())
                .build();
        return new KafkaAdmin.NewTopics(topic);
    }
}
