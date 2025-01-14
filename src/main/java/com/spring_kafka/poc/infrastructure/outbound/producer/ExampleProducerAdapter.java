package com.spring_kafka.poc.infrastructure.outbound.producer;

import com.spring_kafka.poc.config.ExampleAvro;
import com.spring_kafka.poc.infrastructure.outbound.producer.support.default_producer.KafkaDefaultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleProducerAdapter implements ExampleProducer{

    @Value("${spring.kafka.topics.example.topic}")
    private String topic;

    private final KafkaDefaultProducer kafkaDefaultProducer;

    @Override
    public void sendSync(ExampleAvro message) {
        kafkaDefaultProducer.sendMessage(topic, message.getMessage(), message);
    }
}
