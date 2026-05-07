package com.example.coffee_order_system.domain.user.service;

import com.example.coffee_order_system.domain.user.User;
import com.example.coffee_order_system.domain.user.UserRepository;
import com.example.coffee_order_system.domain.user.dto.PointChargeRequestDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceConcurrencyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 테스트 전 0원짜리 유저 1명 생성
        User user = User.builder()
                .name("Test User")
                .point(0L)
                .build();
        User savedUser = userRepository.save(user);
        testUserId = savedUser.getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("포인트 충전 동시성 테스트: 동시에 100번 충전 요청 시 손실 없이 모두 반영되어야 한다")
    void chargePoint_concurrency() throws InterruptedException {
        // given
        int threadCount = 100;
        long chargeAmount = 1000L;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        PointChargeRequestDto requestDto = new PointChargeRequestDto(chargeAmount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    userService.chargePoint(testUserId, requestDto);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        User findUser = userRepository.findById(testUserId).orElseThrow();
        // 100번 * 1000원 = 100,000원
        assertThat(findUser.getPoint()).isEqualTo(100000L);
    }
}
