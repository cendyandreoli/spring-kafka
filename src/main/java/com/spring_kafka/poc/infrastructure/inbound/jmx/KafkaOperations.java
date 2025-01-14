package com.spring_kafka.poc.infrastructure.inbound.jmx;

import com.spring_kafka.poc.config.ExampleAvro;
import com.spring_kafka.poc.infrastructure.outbound.producer.ExampleProducer;
import com.spring_kafka.poc.infrastructure.outbound.producer.support.copy.CopyMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@ManagedResource(objectName = "com.example.kafka:type=KafkaLagManager", description = "Gerenciamento de Lag no Kafka")
@RequiredArgsConstructor
public class KafkaOperations {

    private final AdminClient adminClient;
    private final ExampleProducer exampleProducer;
    private final CopyMessageProducer copyMessageProducer;

    @ManagedOperation(description = "Envia mensagem para o tópico de exemplo")
    @ManagedOperationParameter(name = "message", description = "Mensagem a ser enviada no tópico")
    public void sendMessage(String message) {
        var example = new ExampleAvro(message);
        log.info("Sending message: {}", example);
        exampleProducer.sendSync(example);
        log.info("Message sent successfully: {}", example);
    }

    @ManagedOperation(description = "Obtém o lag de todas as partições de um tópico específico para um grupo de consumidores")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "groupId", description = "ID do grupo de consumidores"),
            @ManagedOperationParameter(name = "topic", description = "Nome do tópico")
    })
    public Map<String, Long> getLagForGroupAndTopic(String groupId, String topic) {
        try {
            log.info("Calculating lag for group '{}' and topic '{}'", groupId, topic);

            var committedOffsetsMap = adminClient
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get();

            var topicOffsets = getTopicOffsets(topic, committedOffsetsMap);

            if (topicOffsets.isEmpty()) {
                log.warn("No committed offsets found for group '{}' and topic '{}'. Returning total message count as lag.", groupId, topic);
                return Map.of("Total", getTotalMessages(topic));
            }

            return calculateLag(topicOffsets);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Operation interrupted while calculating lag: {}", e.getMessage(), e);
            throw new RuntimeException("Operation interrupted", e);
        } catch (ExecutionException e) {
            log.error("Error calculating lag: {}", e.getMessage(), e);
            throw new RuntimeException("Error calculating lag", e);
        }
    }

    @ManagedOperation(description = "Move os offsets de todas as partições de um tópico específico para o final para um grupo de consumidores")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "groupId", description = "ID do grupo de consumidores"),
            @ManagedOperationParameter(name = "topic", description = "Nome do tópico")
    })
    public void moveOffsetsToEndForGroupAndTopic(String groupId, String topic) {
        try {
            log.info("Starting offset movement for group '{}' and topic '{}'", groupId, topic);

            var topicPartitions = getTopicPartitions(topic);
            var offsetsToCommit = getLatestOffsets(topicPartitions);

            if (offsetsToCommit.isEmpty()) {
                log.warn("No offsets found to move for group '{}' and topic '{}'.", groupId, topic);
                return;
            }

            adminClient.alterConsumerGroupOffsets(groupId, offsetsToCommit).all().get();
            log.info("Offsets successfully moved to the end for group '{}' and topic '{}'.", groupId, topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Operation interrupted while moving offsets: {}", e.getMessage(), e);
            throw new RuntimeException("Operation interrupted", e);
        } catch (ExecutionException e) {
            log.error("Error moving offsets: {}", e.getMessage(), e);
            throw new RuntimeException("Error moving offsets", e);
        }
    }

    @ManagedOperation(description = "Copia mensagens do Dead Letter Topic para o tópico principal")
    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "dltTopic", description = "Nome do Dead Letter Topic"),
            @ManagedOperationParameter(name = "mainTopic", description = "Nome do tópico principal")
    })
    public void copyMessage(String dltTopic, String mainTopic) {
        log.info("Iniciando a cópia de mensagens do DLT '{}' para o tópico '{}'", dltTopic, mainTopic);

        try (KafkaConsumer<String, String> consumer = createConsumer(dltTopic)) {
            while (true) {
                var records = consumer.poll(Duration.ofSeconds(1));

                if (records.isEmpty()) {
                    log.info("Nenhuma mensagem encontrada no DLT '{}'.", dltTopic);
                    break;
                }

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        log.info("Mensagem encontrada: key={}, value={}", record.key(), record.value());

                        copyMessageProducer.sendSync(record.value(), mainTopic);

                        log.info("Mensagem copiada com sucesso para o tópico '{}'", mainTopic);
                    } catch (Exception e) {
                        log.error("Erro ao copiar mensagem: {}", e.getMessage(), e);
                    }
                }

                consumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao processar mensagens do DLT '{}': {}", dltTopic, e.getMessage(), e);
            throw new RuntimeException("Erro inesperado ao copiar mensagens do DLT", e);
        }
    }

    private KafkaConsumer<String, String> createConsumer(String dltTopic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "jmx-consumer");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Collections.singletonList(dltTopic));
        return consumer;
    }

    private Map<TopicPartition, Long> getTopicOffsets(String topic, Map<TopicPartition, OffsetAndMetadata> committedOffsetsMap) {
        var topicOffsets = new HashMap<TopicPartition, Long>();
        committedOffsetsMap.forEach((tp, offset) -> {
            if (tp.topic().equals(topic)) {
                topicOffsets.put(tp, offset.offset());
            }
        });
        return topicOffsets;
    }

    private long getTotalMessages(String topic) throws ExecutionException, InterruptedException {
        return adminClient
                .listOffsets(Map.of(new TopicPartition(topic, 0), OffsetSpec.latest()))
                .all()
                .get()
                .values()
                .stream()
                .mapToLong(ListOffsetsResult.ListOffsetsResultInfo::offset)
                .sum();
    }

    private Map<String, Long> calculateLag(Map<TopicPartition, Long> committedOffsets) throws ExecutionException, InterruptedException {
        var latestOffsets = adminClient.listOffsets(getOffsetSpec(committedOffsets.keySet())).all().get();

        var lagMap = new HashMap<String, Long>();
        for (var entry : committedOffsets.entrySet()) {
            var tp = entry.getKey();
            var committedOffset = entry.getValue();
            var latestOffset = latestOffsets.get(tp).offset();
            lagMap.put(tp.topic() + "-" + tp.partition(), Math.max(0, latestOffset - committedOffset));
        }
        return lagMap;
    }

    private Map<TopicPartition, OffsetSpec> getOffsetSpec(Set<TopicPartition> topicPartitions) {
        var request = new HashMap<TopicPartition, OffsetSpec>();
        topicPartitions.forEach(tp -> request.put(tp, OffsetSpec.latest()));
        return request;
    }

    private List<TopicPartition> getTopicPartitions(String topic) throws ExecutionException, InterruptedException {
        var topicDescription = adminClient.describeTopics(List.of(topic)).all().get().get(topic);
        if (topicDescription == null) {
            log.warn("Topic '{}' not found.", topic);
            return Collections.emptyList();
        }
        return topicDescription.partitions()
                .stream()
                .map(partition -> new TopicPartition(topic, partition.partition()))
                .toList();
    }

    private Map<TopicPartition, OffsetAndMetadata> getLatestOffsets(List<TopicPartition> topicPartitions) throws ExecutionException, InterruptedException {
        var latestOffsets = adminClient.listOffsets(getOffsetSpec(new HashSet<>(topicPartitions))).all().get();
        var offsetsToCommit = new HashMap<TopicPartition, OffsetAndMetadata>();
        latestOffsets.forEach((tp, offsetInfo) -> offsetsToCommit.put(tp, new OffsetAndMetadata(offsetInfo.offset())));
        return offsetsToCommit;
    }
}
