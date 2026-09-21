/**
 * [역할] KeywordNotificationService.send() 실행 시간을 구독자 수(N)별로 측정하는 k6 부하 테스트 스크립트
 *
 * [용도]
 *   - 구독자 수가 늘어날수록 응답시간이 선형 증가하는지(N+1 트랜잭션 패턴) 측정
 *   - 수정 전후 비교를 통해 saveAll() 배치 처리 개선 효과 수치화
 *
 * [의존성]
 *   - 서버: LoadTestTriggerController (internal.test.enabled=true 설정 필요)
 *   - DB: seed-keyword-notification.sql 프로시저 등록 및 CALL seed_keyword_notification(N) 실행 필요
 *
 * [필수 환경 변수]
 *   ALLOW_LOAD_TEST=true
 *   BASE_URL           예: http://localhost:8080
 *   TOKEN              ADMIN 권한 JWT (트리거 엔드포인트 인증 필요)
 *
 * [선택 환경 변수]
 *   SUBSCRIBER_COUNT  (기본값: 10)  시드된 구독자 수 (측정 레이블용)
 *   ITERATIONS        (기본값: 10)  반복 실행 횟수
 */

import http from 'k6/http';
import { check, fail } from 'k6';

const baseUrl         = (__ENV.BASE_URL || '').replace(/\/$/, '');
const token           = __ENV.TOKEN || '';
const subscriberCount = __ENV.SUBSCRIBER_COUNT || '10';
const iterations      = Number(__ENV.ITERATIONS || 10);

if (__ENV.ALLOW_LOAD_TEST !== 'true') {
  fail('Refusing to run: set ALLOW_LOAD_TEST=true only after confirming BASE_URL is DEV.');
}
if (!/^https?:\/\//.test(baseUrl)) {
  fail('BASE_URL must be an absolute http(s) URL.');
}
if (!token) {
  fail('TOKEN must be set. The trigger endpoint requires ADMIN role authentication.');
}
if (!Number.isInteger(iterations) || iterations <= 0) {
  fail('ITERATIONS must be a positive integer.');
}

export const options = {
  scenarios: {
    single_trigger: {
      executor: 'shared-iterations',
      vus: 1,
      iterations,
      maxDuration: '10m',
    },
  },
  thresholds: {
    [`http_req_duration{job:trigger,n:${subscriberCount}}`]: ['p(95)<30000', 'p(99)<60000'],
    [`http_req_failed{job:trigger,n:${subscriberCount}}`]:   ['rate<0.01'],
  },
};

const TARGET_PATH = '/internal/test/trigger/keyword-notification';

export default function () {
  const response = http.post(`${baseUrl}${TARGET_PATH}`, null, {
    headers: { 'Authorization': `Bearer ${token}` },
    tags: { job: 'trigger', n: subscriberCount },
    timeout: '120s',
  });

  check(response, {
    'trigger 200': (r) => r.status === 200,
    'not 5xx':     (r) => r.status < 500,
  });
}
