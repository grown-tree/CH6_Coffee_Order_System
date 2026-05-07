package com.example.coffee_order_system.domain.order.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * 주문 완료 이벤트. Kafka 전송을 위해 발행됨.
 */
@Getter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class OrderCompletedEvent {
    private final Long orderId;
    private final Long userId;
    private final Long menuId;
    private final Integer price;
}
