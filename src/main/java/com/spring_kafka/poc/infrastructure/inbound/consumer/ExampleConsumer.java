package com.spring_kafka.poc.infrastructure.inbound.consumer;

import com.spring_kafka.poc.config.Example;
import com.spring_kafka.poc.config.KafkaConfig;
import com.spring_kafka.poc.config.KafkaRetryConsumerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(KafkaConfig.class)
@Slf4j
public class ExampleConsumer {

    @KafkaListener(
            containerFactory = KafkaRetryConsumerConfig.KAFKA_LISTENER_CONTAINER_FACTORY,
            topics = "${spring.kafka.topics.topic-1}",
            groupId = "${spring.kafka.consumer.default-group-id}${spring.kafka.topics.topic-1}"
    )
    public void listen(Example message) {
        log.info("Received message: {}", message);
    }
}
