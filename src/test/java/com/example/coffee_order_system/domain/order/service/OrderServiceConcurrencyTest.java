package com.example.coffee_order_system.domain.order.service;

import com.example.coffee_order_system.domain.menu.Menu;
import com.example.coffee_order_system.domain.menu.MenuRepository;
import com.example.coffee_order_system.domain.order.OrderRepository;
import com.example.coffee_order_system.domain.order.dto.OrderRequestDto;
import com.example.coffee_order_system.domain.user.User;
import com.example.coffee_order_system.domain.user.UserRepository;
import com.example.coffee_order_system.global.exception.BusinessException;
import com.example.coffee_order_system.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long testUserId;
    private Long testMenuId;

    @BeforeEach
    void setUp() {
        // 10,000원 가진 유저 생성
        User user = User.builder()
                .name("Test Buyer")
                .point(10000L)
                .build();
        User savedUser = userRepository.save(user);
        testUserId = savedUser.getId();

        // 3,000원짜리 메뉴 생성
        Menu menu = Menu.builder()
                .name("Americano")
                .price(3000)
                .build();
        Menu savedMenu = menuRepository.save(menu);
        testMenuId = savedMenu.getId();
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        menuRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("커피 주문 동시성 테스트: 10,000원으로 3,000원짜리 커피를 동시에 4번 주문 시, 3번 성공하고 1번 실패하며 최종 잔액은 1,000원이어야 한다")
    void orderCoffee_concurrency() throws InterruptedException {
        // given
        int threadCount = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        OrderRequestDto requestDto = new OrderRequestDto(testUserId, testMenuId);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.orderCoffee(testUserId, requestDto);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_POINT) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        User findUser = userRepository.findById(testUserId).orElseThrow();
        
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(findUser.getPoint()).isEqualTo(1000L); // 10000 - (3000 * 3)
    }
}
