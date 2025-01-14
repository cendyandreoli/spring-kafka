package com.spring_kafka.poc.infrastructure.outbound.producer.support.copy;

public interface CopyMessageProducer {
    void sendSync(String topic, Object message);
}
