-- ============================================================
-- coffee_order_system DDL
-- ============================================================

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    point      BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS menus
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100)   NOT NULL,
    price      INT            NOT NULL,
    created_at DATETIME(6)    NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS orders
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    menu_id    BIGINT       NOT NULL,
    price      INT          NOT NULL,
    ordered_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_menu FOREIGN KEY (menu_id) REFERENCES menus (id),
    INDEX idx_orders_ordered_at (ordered_at)  -- 인기 메뉴 조회 시 날짜 범위 필터용
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
