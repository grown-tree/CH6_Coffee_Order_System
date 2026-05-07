package com.example.coffee_order_system.domain.user.controller;

import com.example.coffee_order_system.domain.user.dto.PointChargeRequestDto;
import com.example.coffee_order_system.domain.user.dto.UserResponseDto;
import com.example.coffee_order_system.domain.user.service.UserService;
import com.example.coffee_order_system.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 포인트 충전 API
     */
    @PostMapping("/{userId}/points/charge")
    public ResponseEntity<ApiResponse<UserResponseDto>> chargePoint(
            @PathVariable Long userId,
            @Valid @RequestBody PointChargeRequestDto requestDto
    ) {
        UserResponseDto responseDto = userService.chargePoint(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }
}
