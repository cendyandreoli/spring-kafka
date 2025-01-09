package com.spring_kafka.poc.infrastructure.inbound.consumer;

import com.spring_kafka.poc.config.Example;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import static com.spring_kafka.poc.config.KafkaRetryConsumerConfig.KAFKA_LISTENER_CONTAINER_FACTORY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleConsumer {

    @KafkaListener(
            topics = "${spring.kafka.topics.topic-1}",
            containerFactory =KAFKA_LISTENER_CONTAINER_FACTORY,
            groupId = "${spring.kafka.topics.group-1}"
    )
    public void listen(Message<Example> transactionEventMessage) {
        log.info("Starting consuming from transaction_events_topic - {}", transactionEventMessage.toString());
    }
}
