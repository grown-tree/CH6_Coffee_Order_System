package com.example.coffee_order_system.domain.user.service;

import com.example.coffee_order_system.domain.user.User;
import com.example.coffee_order_system.domain.user.UserRepository;
import com.example.coffee_order_system.domain.user.dto.PointChargeRequestDto;
import com.example.coffee_order_system.domain.user.dto.UserResponseDto;
import com.example.coffee_order_system.global.exception.BusinessException;
import com.example.coffee_order_system.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 포인트 충전 (비관적 락 사용)
     * 동시 충전 요청이 오더라도 PESSIMISTIC_WRITE 락을 통해 대기 후 순차적으로 포인트를 더한다.
     */
    @Transactional
    public UserResponseDto chargePoint(Long userId, PointChargeRequestDto requestDto) {
        // 1. 비관적 락으로 유저 조회 (없으면 예외)
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 포인트 추가
        user.addPoint(requestDto.getAmount());

        // 3. 응답 DTO 반환
        return UserResponseDto.from(user);
    }
}
