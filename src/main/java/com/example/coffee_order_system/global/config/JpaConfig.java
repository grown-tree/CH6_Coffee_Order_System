package com.example.coffee_order_system.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정
 * - @EnableJpaAuditing: createdAt / updatedAt 자동 관리
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
