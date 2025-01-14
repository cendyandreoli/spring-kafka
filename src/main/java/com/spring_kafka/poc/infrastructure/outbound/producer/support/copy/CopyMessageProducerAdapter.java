package com.spring_kafka.poc.infrastructure.outbound.producer.support.copy;

import com.spring_kafka.poc.infrastructure.outbound.producer.support.default_producer.KafkaDefaultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CopyMessageProducerAdapter implements CopyMessageProducer {

    private final KafkaDefaultProducer kafkaDefaultProducer;

    @Override
    public void sendSync(String topic, Object message) {
        kafkaDefaultProducer.sendMessage(topic, "key", message);
    }
}
