package com.spring_kafka.poc.infrastructure.inbound.jmx;

import com.spring_kafka.poc.config.KafkaProducerConfig;
import com.spring_kafka.poc.infrastructure.outbound.producer.ExampleProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@ManagedResource(objectName = "com.spring_kafka.poc:type=producer", description = "Produzir mensagens no topico")
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaProducerConfig.class)
public class ExampleProducerJmx {

    private final ExampleProducer exampleProducer;

    @ManagedOperation
    @ManagedOperationParameter(name = "message", description = "mensagem a ser enviada no topico")
    public void execute(String message) {
        var example = new com.spring_kafka.poc.config.Example(message);
        log.info("Sending message: {}", example);
        exampleProducer.publishMessage(example);
        log.info("Send message: {}", example);
    }
}
