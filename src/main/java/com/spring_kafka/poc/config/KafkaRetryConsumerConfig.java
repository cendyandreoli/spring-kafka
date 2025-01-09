package com.spring_kafka.poc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnBean(KafkaProducerConfig.class)
public class KafkaRetryConsumerConfig {
    public static final String KAFKA_LISTENER_CONTAINER_FACTORY = "PocSpringKafkaListenerContainerFactory";

    private static final long DEFAULT_RETRY_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(100);
    private static final int DEFAULT_MAX_ATTEMPTS = 10;

    @Bean(name = KAFKA_LISTENER_CONTAINER_FACTORY)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>>
    createRetryKafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {
       return buildContainerFactory(consumerFactory);
    }

    private ConcurrentKafkaListenerContainerFactory<String, Object> buildContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
    
        factory.setConsumerFactory(consumerFactory);
    
        FixedBackOff backOff = new FixedBackOff(
                KafkaRetryConsumerConfig.DEFAULT_RETRY_INTERVAL_MILLIS,
                KafkaRetryConsumerConfig.DEFAULT_MAX_ATTEMPTS
        );

        factory.setAutoStartup(true);
        factory.setCommonErrorHandler(new DefaultErrorHandler(backOff));

        return factory;
    }
}
