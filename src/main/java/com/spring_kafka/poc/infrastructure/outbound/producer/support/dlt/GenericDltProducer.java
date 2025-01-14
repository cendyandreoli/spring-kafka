package com.spring_kafka.poc.infrastructure.outbound.producer.support.dlt;

public interface GenericDltProducer {
    void sendSync(String topic, Object message, String exceptionMessage);
}
