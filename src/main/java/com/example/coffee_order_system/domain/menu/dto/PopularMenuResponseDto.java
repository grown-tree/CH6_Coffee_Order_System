package com.example.coffee_order_system.domain.menu.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PopularMenuResponseDto {

    private final Integer rank;
    private final Long id;
    private final String name;
    private final Long orderCount;

    @Builder
    public PopularMenuResponseDto(Integer rank, Long id, String name, Long orderCount) {
        this.rank = rank;
        this.id = id;
        this.name = name;
        this.orderCount = orderCount;
    }
}
