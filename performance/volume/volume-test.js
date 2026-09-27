import http from 'k6/http';
import { check, fail, sleep } from 'k6';

// 로컬 볼륨/부하 테스트 (performance/volume/API_BOTTLENECK_CANDIDATES.md 후보 API)
//
// MODE
// - single : VU 1명이 DURATION 동안 순차 호출 → 데이터 규모별 단일 요청 응답시간 (볼륨 테스트)
// - rps    : RATE(req/s) 고정 도착률 → 동시 요청 조건 (부하 테스트)
//
// TARGET: 아래 endpoints 키 하나 (예: groups-list, bookshelf, me-trackers)
// TOKEN : mint-token.py로 발급한 로컬 전용 토큰 (heavy = user 1, normal = 일반 유저)

const baseUrl = (__ENV.BASE_URL || 'http://localhost:18080').replace(/\/$/, '');
const token = __ENV.TOKEN || '';
const mode = __ENV.MODE || 'single';
const target = __ENV.TARGET || '';
const nickname = __ENV.NICKNAME || 'reader1';

if (!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(baseUrl)) {
  fail('This script only targets the local volume-test server (http://localhost:<port>).');
}
if (!token) {
  fail('TOKEN is required. Generate one with performance/volume/mint-token.py');
}

const endpoints = {
  'groups-list': '/api/groups?page=0&size=10&sort=LATEST',
  'groups-list-region': `/api/groups?page=0&size=10&sort=LATEST&regions=${encodeURIComponent('서울')}`,
  'groups-search': `/api/groups/search?keyword=${encodeURIComponent('산책')}&page=0&size=10`,
  'groups-home': '/api/groups/home',
  'bookshelf': '/api/mypage/bookshelf',
  'profile': `/api/profiles/${encodeURIComponent(nickname)}`,
  'profile-bookshelf': `/api/profiles/${encodeURIComponent(nickname)}/bookshelf`,
  'me-trackers': '/api/me/trackers',
  'tracker-detail': `/api/trackers/${__ENV.GROUP_ID || '2000001'}/tracker`,
  'library': '/api/library/memberbooks',
  'library-search': `/api/library/memberbooks/search?keyword=${encodeURIComponent('산책')}`,
  'notifications': '/api/notifications?category=SYSTEM&size=20',
  'reviews-written': '/api/mypage/reviews/written?page=0&size=20',
  'reviews-received': '/api/mypage/reviews/received?page=0&size=20',
};

if (!endpoints[target]) {
  fail(`TARGET must be one of: ${Object.keys(endpoints).join(', ')}`);
}

function scenario() {
  if (mode === 'single') {
    // 대용량 단계에서 요청 1건이 1분을 넘어도 끊기지 않도록 종료 대기를 넉넉히 둔다
    return { executor: 'constant-vus', vus: 1, duration: __ENV.DURATION || '1m', gracefulStop: '120s' };
  }
  if (mode === 'rps') {
    return {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 10),
      timeUnit: '1s',
      duration: __ENV.DURATION || '2m',
      preAllocatedVUs: Number(__ENV.PRE_VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 200),
    };
  }
  fail('MODE must be single or rps');
}

export const options = {
  scenarios: { volume: scenario() },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    [`http_req_duration{api:${target}}`]: ['p(95)<500'],
    [`http_req_failed{api:${target}}`]: ['rate<0.01'],
  },
};

const params = {
  headers: { Authorization: `Bearer ${token}` },
  tags: { api: target, test_type: `volume-${mode}` },
  timeout: '120s',
};

export default function () {
  const res = http.get(`${baseUrl}${endpoints[target]}`, params);
  check(res, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });
  if (res.status >= 300 && __ITER < 3) {
    console.warn(`${target} -> ${res.status} ${String(res.body).slice(0, 300)}`);
  }
  if (mode === 'single') sleep(0.2);
}
