-- 홈 인기 도서 집계 테이블 (PopularBook 엔티티)
-- ddl-auto=validate 환경(DEV/PROD)은 배포 전에 수동 적용 필요.
-- groups에서 파생된 읽기용 데이터로, PopularBookScheduler가 10분마다 전체 교체한다 (원본 데이터 아님).
CREATE TABLE IF NOT EXISTS popular_book (
  popular_book_id       BIGINT      NOT NULL AUTO_INCREMENT,
  book_id               BIGINT      NOT NULL,
  group_count           BIGINT      NOT NULL,
  last_group_created_at DATETIME(6) NOT NULL,
  ranking               INT         NOT NULL,
  aggregated_at         DATETIME(6) NOT NULL,
  PRIMARY KEY (popular_book_id),
  UNIQUE KEY uk_popular_book_book (book_id),
  KEY idx_popular_book_ranking (ranking),
  CONSTRAINT fk_popular_book_book FOREIGN KEY (book_id) REFERENCES book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
