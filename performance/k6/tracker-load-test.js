import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const token = __ENV.TOKEN || '';
const groupId = __ENV.GROUP_ID || '';
const meetingGroupId = __ENV.MEETING_GROUP_ID || '';
const deliveryGroupId = __ENV.DELIVERY_GROUP_ID || '';
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
if (!groupId || !meetingGroupId || !deliveryGroupId) {
  fail('GROUP_ID, MEETING_GROUP_ID, DELIVERY_GROUP_ID are all required. Use dedicated DEV fixture groups, not real user groups.');
}
if (!Number.isInteger(vus) || vus <= 0) {
  fail('VUS must be a positive integer.');
}

export const options = {
  vus,
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
  },
};

const headers = {
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
};

// GET 계열: 상태를 변경하지 않아 반복 호출에 안전한 tracker 도메인 엔드포인트.
// - groupId(52): MY_BOOK_READING 상태로 고정된 픽스처
// - meetingGroupId(55): MEETING_SCHEDULED(EXCHANGING) 상태로 고정된 DIRECT 픽스처
// - deliveryGroupId(56): TRACKING_REGISTERED(EXCHANGING) 상태로 고정된 DELIVERY 픽스처
const readEndpoints = [
  '/api/me/trackers',
  `/api/trackers/${groupId}/tracker`,
  `/api/groups/${groupId}/meetings/default-place`,
  `/api/groups/${meetingGroupId}/meetings`,
  `/api/groups/${meetingGroupId}/meetings/default-place`,
  `/api/groups/${deliveryGroupId}/deliveries/address`,
  `/api/groups/${deliveryGroupId}/deliveries/partner`,
];

export default function () {
  const path = readEndpoints[Math.floor(Math.random() * readEndpoints.length)];
  const getRes = http.get(`${baseUrl}${path}`, {
    headers,
    tags: { test_type: 'tracker-read', api_path: path },
  });
  check(getRes, {
    'GET status is not 5xx': (r) => r.status < 500,
    'GET status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });

  // PATCH reading-progress: currentPage는 절대값 기록이라 반복 호출 가능.
  // READING_MAX_PAGE(기본 690)는 TOKEN 계정이 group(GROUP_ID)에서 읽고 있는 책의
  // totalPages보다 작게 맞춰야 완독(다음 단계 전이)이나 INVALID_READING_PROGRESS를 피한다.
  const page = 1 + Math.floor(Math.random() * (readingMaxPage - 1));
  const patchRes = http.patch(
    `${baseUrl}/api/trackers/${groupId}/reading-progress`,
    JSON.stringify({ currentPage: page }),
    { headers, tags: { test_type: 'tracker-write', api_path: '/api/trackers/{groupId}/reading-progress' } }
  );
  check(patchRes, {
    'PATCH reading-progress status is not 5xx': (r) => r.status < 500,
    'PATCH reading-progress status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });

  // PATCH meeting 수정: 장소/시간을 갱신할 뿐 phase를 바꾸지 않아 반복 호출 가능.
  const meetingPatchRes = http.patch(
    `${baseUrl}/api/groups/${meetingGroupId}/meetings`,
    JSON.stringify({
      placeName: 'k6 고정 픽스처 장소',
      address: '서울특별시 강남구 강남대로 396',
      zipCode: '06232',
      x: 127.027621,
      y: 37.497942,
      addressDetail: `k6 load test @ ${Date.now()}`,
      meetingAt: '2026-12-01T14:30:00+09:00',
    }),
    { headers, tags: { test_type: 'tracker-write', api_path: '/api/groups/{groupId}/meetings' } }
  );
  check(meetingPatchRes, {
    'PATCH meetings status is not 5xx': (r) => r.status < 500,
    'PATCH meetings status is 2xx': (r) => r.status >= 200 && r.status < 300,
  });

  sleep(1);
}
