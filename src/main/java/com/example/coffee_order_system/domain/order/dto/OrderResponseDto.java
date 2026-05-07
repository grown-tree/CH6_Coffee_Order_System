package com.example.coffee_order_system.domain.order.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class OrderResponseDto {

    private final Long orderId;
    private final String menuName;
    private final Integer price;
    private final Long remainingPoint;

    @Builder
    public OrderResponseDto(Long orderId, String menuName, Integer price, Long remainingPoint) {
        this.orderId = orderId;
        this.menuName = menuName;
        this.price = price;
        this.remainingPoint = remainingPoint;
    }
}
