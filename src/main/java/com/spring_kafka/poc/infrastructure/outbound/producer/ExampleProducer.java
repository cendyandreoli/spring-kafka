package com.spring_kafka.poc.infrastructure.outbound.producer;

import com.spring_kafka.poc.config.Example;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleProducer {

    @Value( "${spring.kafka.topics.topic-1}")
    private String topic;

    private final KafkaTemplate<String, Example> kafkaTemplate;

    public void publishMessage(Example example) {

        ProducerRecord<String, Example> producerRecord = new ProducerRecord<>(topic, example);
        CompletableFuture<SendResult<String, Example>> completableFuture = kafkaTemplate.send(producerRecord);

        log.info("Sending kafka message on topic {}", topic);

        completableFuture.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka message successfully sent on topic {} and value {}",
                        topic, result.getProducerRecord().value().toString());
            } else {
                log.error("An error occurred while sending kafka message for event with value {}", producerRecord);
            }
        });
    }
}
