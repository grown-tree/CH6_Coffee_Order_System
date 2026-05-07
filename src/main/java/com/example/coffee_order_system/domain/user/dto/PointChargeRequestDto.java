package com.example.coffee_order_system.domain.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointChargeRequestDto {

    @NotNull(message = "충전 금액을 입력해주세요.")
    @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.")
    private Long amount;

    public PointChargeRequestDto(Long amount) {
        this.amount = amount;
    }
}
