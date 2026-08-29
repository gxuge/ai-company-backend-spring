package org.jeecg.modules.system.config;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;

/** 业务行为 Kafka Topic 与消费失败策略。 */
@Configuration
@ConditionalOnProperty(
        prefix = "jeecg.behavior.kafka", name = "enabled", havingValue = "true")
public class TsBehaviorKafkaConfig {

    /** 声明主行为 Topic 和死信 Topic。 */
    @Bean
    public KafkaAdmin.NewTopics tsBehaviorTopics(TsBehaviorConfigBean config) {
        NewTopic topic = TopicBuilder.name(config.getKafka().getTopic())
                .partitions(config.getKafka().getPartitions())
                .replicas(config.getKafka().getReplicas())
                .build();
        NewTopic dlq = TopicBuilder.name(config.getKafka().getDlqTopic())
                .partitions(config.getKafka().getPartitions())
                .replicas(config.getKafka().getReplicas())
                .build();
        return new KafkaAdmin.NewTopics(topic, dlq);
    }

    /** 消费失败重试两次后写入统一死信 Topic。 */
    @Bean
    public CommonErrorHandler tsBehaviorKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            TsBehaviorConfigBean config) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(config.getKafka().getDlqTopic(), record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }
}
