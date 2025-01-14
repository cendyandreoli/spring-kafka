package com.spring_kafka.poc.infrastructure.outbound.producer.support.default_producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDefaultDefaultProducerImpl implements KafkaDefaultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Retryable(
        retryFor = KafkaException.class,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Override
    public <T> void sendMessage(String topic, String key, T message) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
            topic,
            key,
            message
        );

        send(topic, producerRecord);
    }

    public <T> void sendMessage(String topic, T message, Map<String, String> headers) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
            topic,
            message
        );

        headers.forEach((headerKey, headerValue) ->
            producerRecord.headers().add(headerKey, headerValue != null ? headerValue.getBytes() : new byte[0])
        );

        send(topic, producerRecord);
    }

    private void send(String topic, ProducerRecord<String, Object> producerRecord) {
        try {
            final var result = kafkaTemplate.send(producerRecord).get();
            log.info("Kafka message successfully sent to topic '{}' with value '{}'",
                topic, result.getProducerRecord().value().toString());
        } catch (Exception ex) {
            log.error("An error occurred while sending Kafka message for event with value {}",
                producerRecord.value(), ex);
            throw new KafkaException("Failed to send Kafka message", ex);
        }
    }
}
