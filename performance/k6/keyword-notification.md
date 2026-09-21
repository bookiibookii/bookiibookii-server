# KeywordNotificationService saveAndFlush N+1 부하 테스트

> **실행 환경: Windows PowerShell**

## 주의사항

- 트리거 엔드포인트는 `internal.test.enabled=true` 설정 시에만 활성화됨
- 시드 데이터는 `TEST_KN_` prefix로 식별되므로 운영 데이터와 충돌 없음

---

## 시드 프로시저 등록 (최초 1회)

```powershell
Get-Content performance/k6/seed-keyword-notification.sql | mysql -h localhost -u kangyeson -p030722 booki_local
```

---

## 라운드 0 — Baseline (N=0)

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL seed_keyword_notification(0);"
```

```powershell
k6 run --env ALLOW_LOAD_TEST=true `
       --env BASE_URL=http://localhost:8080 `
       --env SUBSCRIBER_COUNT=0 `
       performance/k6/keyword-notification.js
```

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL cleanup_keyword_notification();"
```

---

## 라운드 1 — N=10

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL seed_keyword_notification(10);"
```

```powershell
k6 run --env ALLOW_LOAD_TEST=true `
       --env BASE_URL=http://localhost:8080 `
       --env SUBSCRIBER_COUNT=10 `
       performance/k6/keyword-notification.js
```

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL cleanup_keyword_notification();"
```

---

## 라운드 2 — N=50

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL seed_keyword_notification(50);"
```

```powershell
k6 run --env ALLOW_LOAD_TEST=true `
       --env BASE_URL=http://localhost:8080 `
       --env SUBSCRIBER_COUNT=50 `
       performance/k6/keyword-notification.js
```

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL cleanup_keyword_notification();"
```

---

## 라운드 3 — N=100

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL seed_keyword_notification(100);"
```

```powershell
k6 run --env ALLOW_LOAD_TEST=true `
       --env BASE_URL=http://localhost:8080 `
       --env SUBSCRIBER_COUNT=100 `
       performance/k6/keyword-notification.js
```

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL cleanup_keyword_notification();"
```

---

## 라운드 4 — N=200

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL seed_keyword_notification(200);"
```

```powershell
k6 run --env ALLOW_LOAD_TEST=true `
       --env BASE_URL=http://localhost:8080 `
       --env SUBSCRIBER_COUNT=200 `
       performance/k6/keyword-notification.js
```

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL cleanup_keyword_notification();"
```

---

## 수정 후 재테스트

`saveAndFlush()` → `saveAll()` 배치 INSERT 변경 후 라운드 0~4 동일하게 반복.

---

## 긴급 정리

```powershell
mysql -h localhost -u kangyeson -p030722 booki_local -e "CALL cleanup_keyword_notification();"
```
