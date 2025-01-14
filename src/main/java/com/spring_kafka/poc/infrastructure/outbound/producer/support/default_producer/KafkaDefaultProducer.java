package com.spring_kafka.poc.infrastructure.outbound.producer.support.default_producer;

import java.util.Map;

public interface KafkaDefaultProducer {
    <T> void sendMessage(String topic, String key, T message);
    <T> void sendMessage(String topic, T message, Map<String, String> headers);
}
