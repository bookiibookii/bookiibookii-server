import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const token = __ENV.TOKEN || '';
const groupId = __ENV.GROUP_ID || '';
const vus = Number(__ENV.VUS || 5);
const readingMaxPage = Number(__ENV.READING_MAX_PAGE || 690);

if (__ENV.ALLOW_LOAD_TEST !== 'true') {
  fail('Refusing to run: set ALLOW_LOAD_TEST=true only after confirming BASE_URL is DEV.');
}
if (!/^https?:\/\//.test(baseUrl)) {
  fail('BASE_URL must be an absolute http(s) URL.');
}
if (!token) {
  fail('TOKEN is required.');
}
if (!groupId) {
  fail('GROUP_ID is required. Use a dedicated DEV test group, not a real user group.');
}
if (!Number.isInteger(vus) || vus <= 0) {
  fail('VUS must be a positive integer.');
}

export const options = {
  vus,
  duration: __ENV.DURATION || '1m',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
  },
};

const headers = {
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
};

export default function () {
  // currentPage는 절대값 기록이라 반복 호출에 안전. READING_MAX_PAGE는 TOKEN 계정이
  // GROUP_ID에서 읽는 책의 totalPages보다 작게 맞춰서 완독(다음 단계 전이)을 피한다.
  const page = 1 + Math.floor(Math.random() * (readingMaxPage - 1));
  const response = http.patch(
    `${baseUrl}/api/trackers/${groupId}/reading-progress`,
    JSON.stringify({ currentPage: page }),
    { headers, tags: { test_type: 'dev-write-idempotent' } }
  );

  check(response, {
    'status is not 5xx': (r) => r.status < 500,
  });
  sleep(1);
}
