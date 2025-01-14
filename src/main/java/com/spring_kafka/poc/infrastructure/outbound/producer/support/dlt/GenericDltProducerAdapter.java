package com.spring_kafka.poc.infrastructure.outbound.producer.support.dlt;

import com.spring_kafka.poc.infrastructure.outbound.producer.support.default_producer.KafkaDefaultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenericDltProducerAdapter implements GenericDltProducer {

    private final KafkaDefaultProducer kafkaDefaultProducer;

    @Override
    public void sendSync(String topic, Object message, String exceptionMessage) {

        Map<String, String> headers = new HashMap<>();
        headers.put("exception", exceptionMessage);
        headers.put("occurrenceAt", LocalDateTime.now().toString());

        kafkaDefaultProducer.sendMessage(topic, message, headers);
    }
}
