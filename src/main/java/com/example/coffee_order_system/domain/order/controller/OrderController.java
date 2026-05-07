package com.example.coffee_order_system.domain.order.controller;

import com.example.coffee_order_system.domain.order.dto.OrderRequestDto;
import com.example.coffee_order_system.domain.order.dto.OrderResponseDto;
import com.example.coffee_order_system.domain.order.service.OrderService;
import com.example.coffee_order_system.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 커피 주문 및 결제 API
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> orderCoffee(
            @Valid @RequestBody OrderRequestDto requestDto
    ) {
        OrderResponseDto responseDto = orderService.orderCoffee(requestDto.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responseDto));
    }
}
