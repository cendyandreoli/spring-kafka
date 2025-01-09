package com.spring_kafka.poc.config;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean(name = "KafkaTemplate")
    public KafkaTemplate<String, byte[]> byteArrayKafkaTemplate(
            ProducerFactory<String, byte[]> producerFactory
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        return new KafkaTemplate<>(producerFactory, props);
    }

    @Bean
    @Primary
    @Profile("!test")
    public KafkaTemplate<String, SpecificRecord> kafkaTemplate(
            ProducerFactory<String, SpecificRecord> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
