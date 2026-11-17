package com.finpilot.transaction.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.transaction.domain.OutboxEventEntity;
import com.finpilot.transaction.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@EnableScheduling
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "finpilot.events.kafka-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publish() {
        for (OutboxEventEntity event : outboxEventRepository.findUnpublished()) {
            try {
                Map<String, Object> envelope = new HashMap<>();
                envelope.put("eventType", event.getEventType());
                envelope.put("occurredAt", Instant.now().toString());
                envelope.put("payload", event.getPayload());
                String json = objectMapper.writeValueAsString(envelope);
                String topic = "finpilot.transaction-completed";
                kafkaTemplate.send(topic, event.getAggregateId(), json);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Outbox publish failed id={}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}