package com.payments.ingestion.config;

import com.payments.ingestion.model.dto.PaymentEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.support.ProducerListener;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, PaymentEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // String key — debitAccountId used as partition key
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        // Serialize PaymentEvent to JSON automatically
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonJsonSerializer.class);

        // acks=all: all in-sync replicas must confirm the write.
        // Prevents data loss if the leader crashes mid-write.
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // enable.idempotence: assigns producer ID + sequence number per batch.
        // Broker deduplicates retried batches — no duplicate payments on retry.
        // Requires acks=all to be set.
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // linger.ms: wait up to 5ms to fill a batch before sending.
        // Improves throughput at burst without meaningfully hurting latency.
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        // compression.type: snappy gives ~2-3x compression with low CPU cost.
        // Reduces broker disk I/O and network at high payment volumes.
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // retries: auto-retry up to 3 times on transient broker errors.
        // Safe because idempotence is enabled — no duplicates.
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // delivery.timeout.ms: total time allowed for a send including retries.
        // Must be larger than linger.ms + request.timeout.ms (default 30000).
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);

        // Add type mapping so the consumer knows which class to deserialize to
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        config.put(JacksonJsonSerializer.TYPE_MAPPINGS,
                "paymentEvent:com.payments.ingestion.model.dto.PaymentEvent");

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate() {
        KafkaTemplate<String, PaymentEvent> template =
                new KafkaTemplate<>(producerFactory());

        // Log every successful send at DEBUG level — useful during smoke test
        template.setProducerListener(new ProducerListener<>() {
            @Override
            public void onSuccess(ProducerRecord<String, PaymentEvent> record,
                                  RecordMetadata metadata) {
                log.debug("Published paymentId={} to partition={}",
                        record.value().getPaymentId(),
                        metadata.partition());
            }

            @Override
            public void onError(ProducerRecord<String, PaymentEvent> record,
                                RecordMetadata metadata,
                                Exception ex) {
                log.error("Kafka publish failed for paymentId={} error={}",
                        record.value().getPaymentId(), ex.getMessage());
            }
        });

        return template;
    }
}
