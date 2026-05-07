package com.example.coffee_order_system.domain.order.service;

import com.example.coffee_order_system.domain.menu.Menu;
import com.example.coffee_order_system.domain.menu.MenuRepository;
import com.example.coffee_order_system.domain.order.Order;
import com.example.coffee_order_system.domain.order.OrderRepository;
import com.example.coffee_order_system.domain.order.dto.OrderRequestDto;
import com.example.coffee_order_system.domain.order.dto.OrderResponseDto;
import com.example.coffee_order_system.domain.order.event.OrderCompletedEvent;
import com.example.coffee_order_system.domain.user.User;
import com.example.coffee_order_system.domain.user.UserRepository;
import com.example.coffee_order_system.global.exception.BusinessException;
import com.example.coffee_order_system.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    
    // Kafka 이벤트를 발행할 이벤트 퍼블리셔
    private final ApplicationEventPublisher eventPublisher;
    // 인기 메뉴 ZINCRBY를 위한 StringRedisTemplate
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public OrderResponseDto orderCoffee(Long userId, OrderRequestDto requestDto) {
        // 1. 유저 조회 (비관적 락으로 동시성 제어)
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 메뉴 조회
        Menu menu = menuRepository.findById(requestDto.getMenuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        // 3. 포인트 잔액 검증
        if (user.getPoint() < menu.getPrice()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        // 4. 포인트 차감 및 주문 생성 (동일 트랜잭션)
        user.deductPoint((long) menu.getPrice());
        
        Order order = Order.builder()
                .user(user)
                .menu(menu)
                .price(menu.getPrice()) // 주문 당시 가격 스냅샷
                .build();
        orderRepository.save(order);

        // 5. 인기 메뉴 점수 갱신 (Redis ZINCRBY) - 폴백 처리
        incrementPopularMenuScore(menu.getId());

        // 6. 주문 완료 이벤트 발행 (AFTER_COMMIT 시점에 Kafka로 전송됨)
        eventPublisher.publishEvent(new OrderCompletedEvent(
                order.getId(), user.getId(), menu.getId(), menu.getPrice()
        ));

        // 7. 응답 생성
        return OrderResponseDto.builder()
                .orderId(order.getId())
                .menuName(menu.getName())
                .price(menu.getPrice())
                .remainingPoint(user.getPoint())
                .build();
    }

    /**
     * Redis ZSET 기반 인기 메뉴 누적.
     * Redis 장애 시 주문 트랜잭션에 영향을 주지 않도록 예외를 잡아서 로깅만 처리 (폴백)
     */
    private void incrementPopularMenuScore(Long menuId) {
        try {
            String dateString = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
            String redisKey = "popular:menu:" + dateString;
            
            // ZINCRBY 실행. 키가 없으면 자동 생성 후 1 추가.
            stringRedisTemplate.opsForZSet().incrementScore(redisKey, String.valueOf(menuId), 1.0);
            // 만료일 설정 (7일 집계 + 1일 여유 = 8일)
            stringRedisTemplate.expire(redisKey, 8, TimeUnit.DAYS);
            
        } catch (Exception e) {
            log.error("[Redis Fallback] 인기 메뉴 ZINCRBY 갱신 실패. 주문 처리는 계속 진행됩니다. 에러: {}", e.getMessage());
        }
    }
}
