package com.example.coffee_order_system.domain.menu.service;

import com.example.coffee_order_system.domain.menu.Menu;
import com.example.coffee_order_system.domain.menu.MenuRepository;
import com.example.coffee_order_system.domain.menu.dto.PopularMenuResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuService {

    private final StringRedisTemplate stringRedisTemplate;
    private final MenuRepository menuRepository;

    private static final String POPULAR_MENU_RESULT_KEY = "popular:menu:result";
    private static final int DAYS_TO_COLLECT = 7;
    private static final int RESULT_CACHE_TTL_MINUTES = 5;

    /**
     * 최근 7일간의 인기 메뉴 TOP 3 조회
     */
    public List<PopularMenuResponseDto> getPopularMenus() {
        try {
            // 1. ZUNIONSTORE 결과 키가 존재하는지 확인 (5분 캐시)
            Boolean hasKey = stringRedisTemplate.hasKey(POPULAR_MENU_RESULT_KEY);
            
            if (Boolean.FALSE.equals(hasKey)) {
                // 결과 키가 없으면 7일치 키를 생성하여 ZUNIONSTORE 수행
                List<String> keys = new ArrayList<>();
                LocalDate now = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
                
                for (int i = 0; i < DAYS_TO_COLLECT; i++) {
                    keys.add("popular:menu:" + now.minusDays(i).format(formatter));
                }

                String baseKey = keys.get(0);
                List<String> otherKeys = keys.subList(1, keys.size());

                // ZUNIONSTORE 실행
                stringRedisTemplate.opsForZSet().unionAndStore(baseKey, otherKeys, POPULAR_MENU_RESULT_KEY);
                
                // 결과에 EXPIRE 5분 설정 (불필요한 연산 방지)
                stringRedisTemplate.expire(POPULAR_MENU_RESULT_KEY, RESULT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }

            // 2. 상위 3개 가져오기 (ZREVRANGE WITHSCORES)
            Set<ZSetOperations.TypedTuple<String>> top3 = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(POPULAR_MENU_RESULT_KEY, 0, 2);

            if (top3 == null || top3.isEmpty()) {
                return Collections.emptyList(); // 데이터가 없는 경우
            }

            // 3. 메뉴 정보 조회를 위해 ID 추출
            List<Long> menuIds = top3.stream()
                    .map(tuple -> Long.parseLong(tuple.getValue()))
                    .collect(Collectors.toList());

            // 4. DB에서 메뉴 이름 조회 (IN 쿼리로 한번에)
            Map<Long, String> menuNameMap = menuRepository.findAllById(menuIds).stream()
                    .collect(Collectors.toMap(Menu::getId, Menu::getName));

            // 5. 최종 DTO 생성 (랭킹 매기기)
            List<PopularMenuResponseDto> result = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : top3) {
                Long menuId = Long.parseLong(tuple.getValue());
                Long count = tuple.getScore() != null ? tuple.getScore().longValue() : 0L;
                String name = menuNameMap.getOrDefault(menuId, "Unknown Menu");

                result.add(PopularMenuResponseDto.builder()
                        .rank(rank++)
                        .id(menuId)
                        .name(name)
                        .orderCount(count)
                        .build());
            }

            return result;

        } catch (Exception e) {
            // Redis 연결 실패 등 장애 시 폴백(Fallback): 빈 리스트 반환
            log.error("[Redis Fallback] 인기 메뉴 조회 실패. 빈 리스트를 반환합니다. 에러: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
