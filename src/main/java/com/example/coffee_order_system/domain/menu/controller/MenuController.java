package com.example.coffee_order_system.domain.menu.controller;

import com.example.coffee_order_system.domain.menu.dto.MenuResponseDto;
import com.example.coffee_order_system.domain.menu.service.MenuService;
import com.example.coffee_order_system.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 커피 메뉴 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponseDto>>> getMenus() {
        List<MenuResponseDto> menus = menuService.getAllMenus();
        return ResponseEntity.ok(ApiResponse.ok(menus));
    }
}
