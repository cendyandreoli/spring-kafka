package com.spring_kafka.poc.infrastructure.inbound.consumer;

import com.spring_kafka.poc.config.ExampleAvro;
import com.spring_kafka.poc.infrastructure.outbound.producer.support.dlt.GenericDltProducer;
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

    private final GenericDltProducer genericDltProducer;

    @KafkaListener(
            topics = "${spring.kafka.topics.example.topic}",
            containerFactory =KAFKA_LISTENER_CONTAINER_FACTORY,
            groupId = "${spring.kafka.topics.example.group}"
    )
    public void listen(Message<ExampleAvro> exampleMessage) {
        try {
            log.info("Starting consuming from example_topic - {}", exampleMessage.toString());
            if ("fail".equals(exampleMessage.getPayload().getMessage())) {
                throw new Exception("error");
            }
        } catch (Exception e) {
            log.error("Exception when process message from example_topic - {}", exampleMessage.toString());
            genericDltProducer.sendSync(
                    "topico-exemplo-1-dlt",
                    exampleMessage.getPayload(),
                    e.toString()
            );
        }

    }
}
