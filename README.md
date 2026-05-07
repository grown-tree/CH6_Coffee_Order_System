# ☕ Coffee Order System

다수 서버 환경에서도 안정적으로 동작하는 커피숍 주문 시스템입니다.

---

## 📐 ERD

```
┌──────────────────────────────────────────────────────────────────────┐
│  users                              menus                            │
│ ─────────────────────────          ─────────────────────────         │
│  id          BIGINT (PK)            id          BIGINT (PK)          │
│  name        VARCHAR(100)           name        VARCHAR(100)         │
│  point       BIGINT                 price       INT                  │
│  created_at  DATETIME               created_at  DATETIME             │
│  updated_at  DATETIME                                                │
│       │                                   │                          │
│       └──────────────┐   ┌────────────────┘                          │
│                      ▼   ▼                                           │
│                     orders                                           │
│                    ─────────────────────────                         │
│                     id          BIGINT (PK)                          │
│                     user_id     BIGINT (FK → users)                  │
│                     menu_id     BIGINT (FK → menus)                  │
│                     price       INT                                  │
│                     ordered_at  DATETIME  ← INDEX                    │
└──────────────────────────────────────────────────────────────────────┘
```

> **설계 보충**
> - `orders.price`: 주문 시점의 가격 스냅샷입니다. `menus.price`는 언제든 변경될 수 있으므로 주문 당시의 실제 결제 금액을 별도로 저장합니다.

---

## 📋 API 명세서

> **인증/인가**: 본 과제는 핵심 비즈니스 로직(동시성, 데이터 일관성, 확장성)에 집중하기 위해 별도의 인증 없이 `userId`를 요청 파라미터로 직접 받습니다. 실제 운영 환경에서는 JWT 등을 통한 인증 레이어 추가가 필요합니다.

### 1. 메뉴 목록 조회

| 항목     | 내용                     |
|--------|--------------------------|
| Method | `GET`                    |
| URI    | `/api/menus`             |
| Response | `[{ id, name, price }]` |

```json
// 200 OK
{
  "success": true,
  "data": [
    { "id": 1, "name": "아메리카노", "price": 3000 },
    { "id": 2, "name": "카페라떼",   "price": 4500 }
  ]
}
```

---

### 2. 포인트 충전

| 항목     | 내용                                |
|--------|-------------------------------------|
| Method | `POST`                              |
| URI    | `/api/users/{userId}/points/charge` |
| Body   | `{ "amount": 10000 }`               |
| Response | `{ userId, remainingPoint }`      |

```json
// 200 OK
{
  "success": true,
  "data": { "userId": 1, "remainingPoint": 20000 }
}

// 400 Bad Request
{
  "success": false,
  "code": "INVALID_CHARGE_AMOUNT",
  "message": "충전 금액은 1원 이상이어야 합니다."
}
```

---

### 3. 커피 주문/결제

| 항목     | 내용                              |
|--------|-----------------------------------|
| Method | `POST`                            |
| URI    | `/api/orders`                     |
| Body   | `{ "userId": 1, "menuId": 2 }`    |
| Response | `{ orderId, menuName, price, remainingPoint }` |

```json
// 201 Created
{
  "success": true,
  "data": {
    "orderId": 42,
    "menuName": "카페라떼",
    "price": 4500,
    "remainingPoint": 5500
  }
}

// 400 Bad Request
{
  "success": false,
  "code": "INSUFFICIENT_POINT",
  "message": "포인트가 부족합니다."
}
```

---

### 4. 인기 메뉴 목록 조회

| 항목     | 내용                          |
|--------|-------------------------------|
| Method | `GET`                         |
| URI    | `/api/menus/popular`          |
| Response | 최근 7일 주문 TOP 3 메뉴      |

```json
// 200 OK
{
  "success": true,
  "data": [
    { "rank": 1, "id": 1, "name": "아메리카노", "orderCount": 152 },
    { "rank": 2, "id": 2, "name": "카페라떼",   "orderCount": 98  },
    { "rank": 3, "id": 3, "name": "카푸치노",   "orderCount": 74  }
  ]
}
```

---

## 🏗️ 설계 의도

### 패키지 구조

```
com.example.coffee_order_system
├── domain
│   ├── user        ← User 엔티티, 포인트 충전 서비스
│   ├── menu        ← Menu 엔티티, 메뉴 조회 서비스
│   └── order       ← Order 엔티티, 주문 서비스, Kafka Producer/Consumer
└── global
    ├── config      ← JPA, Redis, Kafka 설정
    ├── exception   ← ErrorCode, BusinessException, GlobalExceptionHandler
    └── response    ← ApiResponse (공통 응답 포맷)
```

도메인별로 패키지를 분리하여 각 도메인의 책임을 명확히 합니다.
`global` 패키지는 여러 도메인에서 공통으로 사용하는 인프라 코드를 담습니다.

---

## 🎯 도전 요구사항 대응

| 요구사항 | 적용 전략 |
|---------|-----------|
| 다중 인스턴스 안전성 | 공유 저장소(DB, Redis, Kafka) 기반 Stateless 설계 — 로컬 상태 없음 |
| 동시성 이슈 | 비관적 락(`SELECT FOR UPDATE`)으로 포인트 충전/차감 원자적 처리 |
| 데이터 일관성 | `@TransactionalEventListener(AFTER_COMMIT)`으로 트랜잭션과 Kafka 분리 |
| 테스트 | `CountDownLatch` 기반 멀티스레드 동시성 통합 테스트 작성 |

---

## ⚙️ 기술적 선택 이유

### 1. 포인트 충전/차감 — 비관적 락 (Pessimistic Lock)

**문제**: 다수 인스턴스 환경에서 동시에 포인트를 충전하거나 차감하면 데이터 정합성이 깨질 수 있습니다.

**분석**:

| 전략 | 장점 | 단점 |
|------|------|------|
| 낙관적 락 (Optimistic) | 락 없이 처리, 경합 없을 때 빠름 | 충돌 시 재시도 필요, 재시도 로직 복잡도 증가 |
| **비관적 락 (Pessimistic)** | 충돌 자체를 방지, 단순한 구현 | 트랜잭션 점유 시간 증가 |

**선택: 비관적 락**

커피 주문 시스템에서 포인트는 금전과 동일한 중요도를 가집니다.
재시도가 실패하면 포인트가 유실될 수 있는 낙관적 락보다, 대기 후 정확하게 처리하는 비관적 락이 더 안전합니다.
주문 빈도가 매우 높지 않은 카페 시스템 특성상 락 경합이 극단적으로 크지 않아 선택했습니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findByIdWithLock(Long id);
```

---

### 2. 인기 메뉴 집계 — Redis ZSET (일별 키 + ZUNIONSTORE)

**문제**: 최근 7일 인기 메뉴를 빠르고 정확하게 조회해야 합니다.

**분석**:

| 전략 | 정확도 | 속도 | 다중 인스턴스 |
|------|--------|------|--------------|
| DB GROUP BY 쿼리 | ✅ 정확 | ⚠️ 조회마다 DB 쿼리 실행 | ✅ 단일 DB |
| DB + Redis 캐시 | ✅ 정확 | ✅ 캐시 히트 시 빠름 | ⚠️ 캐시 갱신 타이밍 |
| **Redis ZSET** | ✅ 정확 | ✅ 항상 빠름 | ✅ 원자 연산 |

**선택: Redis ZSET**

```
# 주문 발생 시 (원자적 연산 - 다중 인스턴스 안전)
ZINCRBY popular:menu:20260507 1 "menuId:1"

# 인기 메뉴 조회 시
ZUNIONSTORE popular:menu:result 7 \
  popular:menu:20260501 ... popular:menu:20260507

ZREVRANGE popular:menu:result 0 2 WITHSCORES
```

- `ZINCRBY`는 Redis 원자 연산 → 다중 인스턴스에서 락 없이 정확한 카운팅
- 일별 키에 `EXPIRE 8일` → 자동 데이터 만료, 메모리 관리 자동화
- `ZUNIONSTORE` 결과에 `EXPIRE 5분` → 반복 조회 시 성능 최적화

**Redis 장애 시 폴백 전략**

Redis는 인기 메뉴 집계의 단일 저장소이므로 장애 시 `ZINCRBY`와 인기 메뉴 조회가 불가합니다.
- **주문 자체는 정상 처리**: 포인트 차감은 DB 비관적 락 기반이므로 Redis 장애와 무관합니다.
- **ZINCRBY 실패**: try-catch로 감싸 Kafka 전송과 마찬가지로 주문 트랜잭션에 영향을 주지 않도록 처리합니다. 에러 로그 기록으로 추적합니다.
- **인기 메뉴 조회 실패**: 빈 리스트 또는 캐시 만료 응답 반환으로 Graceful Degradation 처리합니다.

---

### 3. 주문 데이터 실시간 전송 — Kafka

**문제**: 주문 완료 시 데이터 수집 플랫폼으로 실시간 전송이 필요합니다.

**선택: Kafka (`AFTER_COMMIT` 이벤트 기반 비동기 전송)**

```
주문 서비스 ──[트랜잭션 커밋]──▶ ApplicationEvent 발행
                                      │
                                      ▼ (@TransactionalEventListener AFTER_COMMIT)
                             Kafka Producer ──▶ order-events 토픽
                                                    │
                                                    ▼
                                           Data Platform Consumer
```

- 주문 트랜잭션과 Kafka 전송을 분리 → Kafka 전송 실패가 주문 실패로 이어지지 않음
- `acks=all` 설정으로 메시지 유실 방지
- `@TransactionalEventListener(AFTER_COMMIT)` → 트랜잭션 롤백된 주문의 이벤트는 미전송

**Kafka 전송 실패 처리 전략**

Kafka 전송 실패는 주문 트랜잭션과 분리되어 있으므로 주문 자체에는 영향을 주지 않습니다.
전송 실패 시 에러 로그를 기록하여 추적 가능하게 하며, 필요 시 DLT(Dead Letter Topic)를 도입하여 실패 메시지를 별도 보관하고 재처리할 수 있도록 확장 가능합니다.

---

### 4. 다중 인스턴스 안전성

| 컴포넌트 | 전략 |
|---------|------|
| 포인트 충전/차감 | DB 비관적 락 (공유 DB가 단일 진실 소스) |
| 인기 메뉴 카운팅 | Redis ZINCRBY (원자 연산, 로컬 상태 없음) |
| 주문 이벤트 전송 | Kafka (파티션 3개, 다중 컨슈머 병렬 처리) |
| 세션/상태 | 로컬 상태 없음 (Stateless) |

모든 상태를 공유 저장소(DB, Redis, Kafka)에서 관리하므로
인스턴스를 수평 확장(Scale-out)해도 기능에 문제가 없습니다.

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.8 |
| ORM | Spring Data JPA / Hibernate |
| DB | MySQL 8.x |
| Cache / 집계 | Redis 7.x (ZSET) |
| Message Broker | Apache Kafka |
| Build | Gradle |

---

## 🚀 실행 방법

### 사전 준비
```bash
# MySQL
CREATE DATABASE coffee_order_db CHARACTER SET utf8mb4;

# Redis & Kafka (Docker Compose 사용 시)
docker-compose up -d
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### 테스트
```bash
./gradlew test
```