package com.example.coffee_order_system.domain.order.kafka;

import com.example.coffee_order_system.domain.order.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.order}")
    private String topic;

    /**
     * 주문 트랜잭션 커밋이 성공적으로 완료된 후 (AFTER_COMMIT)
     * 별도의 스레드(@Async)에서 Kafka 전송을 수행.
     * 이를 통해 Kafka 지연이 클라이언트 응답이나 트랜잭션 롤백에 영향을 주지 않음.
     */
    @Async("kafkaAsyncExecutor")//별도 스레드풀에서 실행하여 http응답속도 일정하게 유지
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)//주문 트랜잭션이 완전히 커밋된 후에만 실행
    public void sendOrderEvent(OrderCompletedEvent event) {
        log.info("[Kafka Producer] 주문 이벤트 전송 시작 - orderId: {}", event.getOrderId());
        
        try {
            // 메시지 키를 orderId로 하여 동일 주문의 이벤트 순서 보장 (여기선 단일 이벤트지만 관례상 설정)
            kafkaTemplate.send(topic, String.valueOf(event.getOrderId()), event);
            log.info("[Kafka Producer] 주문 이벤트 전송 성공 - 토픽: {}", topic);
        } catch (Exception e) {
            // 전송 실패 시 에러 로깅. DLT(Dead Letter Topic) 또는 재시도 로직으로 확장 가능
            log.error("[Kafka Producer] 주문 이벤트 전송 실패 - orderId: {}, 에러: {}", event.getOrderId(), e.getMessage());
        }
    }
}
