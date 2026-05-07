package com.example.coffee_order_system.domain.menu.dto;

import com.example.coffee_order_system.domain.menu.Menu;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MenuResponseDto {

    private final Long id;
    private final String name;
    private final Integer price;

    @Builder
    public MenuResponseDto(Long id, String name, Integer price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public static MenuResponseDto from(Menu menu) {
        return MenuResponseDto.builder()
                .id(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .build();
    }
}
