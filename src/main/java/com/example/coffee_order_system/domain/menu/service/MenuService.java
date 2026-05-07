package com.example.coffee_order_system.domain.menu.service;

import com.example.coffee_order_system.domain.menu.MenuRepository;
import com.example.coffee_order_system.domain.menu.dto.MenuResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    /**
     * 전체 커피 메뉴 목록 조회
     */
    public List<MenuResponseDto> getAllMenus() {
        return menuRepository.findAll().stream()
                .map(MenuResponseDto::from)
                .collect(Collectors.toList());
    }
}
