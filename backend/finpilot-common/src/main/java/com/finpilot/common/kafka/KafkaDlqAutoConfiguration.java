package com.finpilot.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(name = "finpilot.events.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDlqAutoConfiguration {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new TopicPartition(record.topic() + ".dlq", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}