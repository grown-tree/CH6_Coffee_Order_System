package com.example.coffee_order_system.domain.order.kafka;

import com.example.coffee_order_system.domain.order.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderKafkaConsumer {

    /**
     * 데이터 수집 플랫폼을 모방하는 Mock Consumer
     */
    @KafkaListener(topics = "${app.kafka.topic.order}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderCompletedEvent event) {
        log.info("[Mock Data Platform] 수집 완료 - orderId: {}, userId: {}, menuId: {}, price: {}",
                event.getOrderId(), event.getUserId(), event.getMenuId(), event.getPrice());
    }
}
