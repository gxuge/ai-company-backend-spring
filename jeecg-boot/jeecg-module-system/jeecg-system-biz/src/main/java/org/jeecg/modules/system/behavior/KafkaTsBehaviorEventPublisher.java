package org.jeecg.modules.system.behavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** 基于 Kafka 的业务行为消息发布器。 */
@Slf4j
@Component
public class KafkaTsBehaviorEventPublisher implements TsBehaviorEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TsBehaviorConfigBean config;

    /** 注入 Kafka、JSON 和行为配置。 */
    public KafkaTsBehaviorEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            TsBehaviorConfigBean config) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /** {@inheritDoc} */
    @Override
    public void publish(TsBehaviorEventMessage event) {
        if (!config.getKafka().isEnabled()) {
            throw new JeecgBootException("业务行为采集未启用");
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(config.getKafka().getTopic(), event.getUserId(), payload)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.error(
                                    "业务行为事件异步发送失败，eventId={}",
                                    event.getEventId(),
                                    error);
                        }
                    });
        } catch (JsonProcessingException exception) {
            throw new JeecgBootException("业务行为事件序列化失败", exception);
        } catch (RuntimeException exception) {
            throw new JeecgBootException("业务行为事件提交失败", exception);
        }
    }
}
