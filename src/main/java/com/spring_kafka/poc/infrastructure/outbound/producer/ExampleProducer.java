package com.spring_kafka.poc.infrastructure.outbound.producer;


import com.spring_kafka.poc.config.ExampleAvro;

public interface ExampleProducer {
    void sendSync(ExampleAvro message);
}
