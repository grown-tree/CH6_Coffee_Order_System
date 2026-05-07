package com.example.coffee_order_system.domain.user.dto;

import com.example.coffee_order_system.domain.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserResponseDto {

    private final Long userId;
    private final Long remainingPoint;

    @Builder
    public UserResponseDto(Long userId, Long remainingPoint) {
        this.userId = userId;
        this.remainingPoint = remainingPoint;
    }

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .userId(user.getId())
                .remainingPoint(user.getPoint())
                .build();
    }
}
