package com.example.coffee_order_system.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 설정
 *
 * <p>주문 이벤트 토픽을 애플리케이션 시작 시 자동 생성한다.
 * <ul>
 *   <li>partition: 3 — 다중 인스턴스 병렬 처리 고려</li>
 *   <li>replicas: 1  — 로컬 개발 환경 기준 (운영 시 3으로 변경 권장)</li>
 * </ul>
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.order}")
    private String orderTopic;

    @Bean
    public NewTopic orderEventTopic() {
        return TopicBuilder.name(orderTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
