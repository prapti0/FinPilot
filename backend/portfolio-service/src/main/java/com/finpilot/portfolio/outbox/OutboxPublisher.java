package com.finpilot.portfolio.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.portfolio.domain.OutboxEventEntity;
import com.finpilot.portfolio.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
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
                Map<String, Object> stored = event.getPayload();
                String topic = (String) stored.getOrDefault("kafkaTopic", "finpilot.portfolio-updated");
                Object payload = stored.get("payload");
                Map<String, Object> envelope = new HashMap<>();
                envelope.put("eventType", event.getEventType());
                envelope.put("occurredAt", Instant.now().toString());
                envelope.put("payload", payload);
                String json = objectMapper.writeValueAsString(envelope);
                kafkaTemplate.send(topic, event.getAggregateId(), json);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                log.warn("Outbox publish failed id={}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}