import http from 'k6/http';
import { check, fail } from 'k6';

// Tracker 도메인 성능/부하/스트레스 테스트 (open model, 도착률 기반).
//
// MODE
// - rps    : RATE(req/s)를 DURATION 동안 고정. 단계 부하테스트(10/50/100/200/300)용. 단계마다 따로 실행.
// - stress : 0 → MAX_RATE까지 1분마다 STEP_RATE씩 올리며 한계점 탐색. 임계치 초과 시 자동 중단.
//
// TARGET
// - mix               : 아래 조회 API를 가중치대로 섞어서 호출 (1차 스크리닝용)
// - me-trackers 등 키 : 단일 API만 호출 (문제 API 격리 측정용)

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const token = __ENV.TOKEN || '';
const groupId = __ENV.GROUP_ID || '';
const meetingGroupId = __ENV.MEETING_GROUP_ID || '';
const deliveryGroupId = __ENV.DELIVERY_GROUP_ID || '';
const readingMaxPage = Number(__ENV.READING_MAX_PAGE || 495);
const mode = __ENV.MODE || 'rps';
const target = __ENV.TARGET || 'mix';
const rate = Number(__ENV.RATE || 10);
const maxRate = Number(__ENV.MAX_RATE || 400);
const stepRate = Number(__ENV.STEP_RATE || 50);

if (__ENV.ALLOW_LOAD_TEST !== 'true') {
  fail('Refusing to run: set ALLOW_LOAD_TEST=true only after confirming BASE_URL is DEV.');
}
// Bearer 토큰이 평문으로 노출되지 않도록 원격 서버는 https만 허용하고, http는 로컬 루프백 주소에서만 허용한다.
const isLoopbackHttp = /^http:\/\/(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$/.test(baseUrl);
if (!/^https:\/\//.test(baseUrl) && !isLoopbackHttp) {
  fail('BASE_URL must be an absolute https URL (http is allowed only for localhost).');
}
if (!token) {
  fail('TOKEN is required.');
}
if (!groupId || !meetingGroupId) {
  fail('GROUP_ID, MEETING_GROUP_ID are required. Use dedicated DEV fixture groups, not real user groups.');
}

// weight: mix 모드에서의 호출 비중 (홈/트래커 화면 진입 빈도 가정)
const endpoints = {
  'me-trackers': { weight: 40, method: 'GET', path: '/api/me/trackers' },
  'tracker-detail': { weight: 30, method: 'GET', path: `/api/trackers/${groupId}/tracker` },
  'reading-progress': {
    weight: 15,
    method: 'PATCH',
    path: `/api/trackers/${groupId}/reading-progress`,
    // currentPage는 절대값 기록이라 반복 호출에 안전. totalPages 미만으로 유지해 완독 전이를 피한다.
    body: () => JSON.stringify({ currentPage: 1 + Math.floor(Math.random() * (readingMaxPage - 1)) }),
  },
  'meetings': { weight: 10, method: 'GET', path: `/api/groups/${meetingGroupId}/meetings` },
  'default-place': { weight: 5, method: 'GET', path: `/api/groups/${meetingGroupId}/meetings/default-place` },
};
if (deliveryGroupId) {
  endpoints['deliveries-address'] = { weight: 5, method: 'GET', path: `/api/groups/${deliveryGroupId}/deliveries/address` };
  endpoints['deliveries-partner'] = { weight: 5, method: 'GET', path: `/api/groups/${deliveryGroupId}/deliveries/partner` };
}

if (target !== 'mix' && !endpoints[target]) {
  fail(`Unknown TARGET "${target}". Use mix or one of: ${Object.keys(endpoints).join(', ')}`);
}

const activeNames = target === 'mix' ? Object.keys(endpoints) : [target];
const totalWeight = activeNames.reduce((sum, name) => sum + endpoints[name].weight, 0);

function pickEndpoint() {
  let r = Math.random() * totalWeight;
  for (const name of activeNames) {
    r -= endpoints[name].weight;
    if (r < 0) return name;
  }
  return activeNames[activeNames.length - 1];
}

function buildScenario() {
  const preAllocatedVUs = Number(__ENV.PRE_VUS || 50);
  const maxVUs = Number(__ENV.MAX_VUS || 300);
  if (mode === 'rps') {
    return {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration: __ENV.DURATION || '2m',
      preAllocatedVUs,
      maxVUs,
    };
  }
  if (mode === 'stress') {
    // STEP_RATE가 0 이하이거나 숫자가 아니면 아래 반복문이 끝나지 않으므로 먼저 검증한다.
    if (!Number.isInteger(stepRate) || stepRate <= 0) {
      fail('STEP_RATE must be a positive integer.');
    }
    if (!Number.isInteger(maxRate) || maxRate < stepRate) {
      fail('MAX_RATE must be an integer greater than or equal to STEP_RATE.');
    }
    const stages = [];
    for (let r = stepRate; r <= maxRate; r += stepRate) {
      stages.push({ target: r, duration: '15s' }); // 램프
      stages.push({ target: r, duration: '45s' }); // 유지
    }
    stages.push({ target: 0, duration: '30s' });
    return {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      stages,
      preAllocatedVUs,
      maxVUs,
    };
  }
  fail(`Unknown MODE "${mode}". Use rps or stress.`);
}

// API별 p95/실패율이 summary에 따로 찍히도록 태그별 threshold를 등록한다.
const thresholds = {
  // 안전장치: 공유 DEV 서버 보호용 자동 중단
  http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '20s' }],
  http_req_duration: [{ threshold: 'p(95)<2000', abortOnFail: true, delayAbortEval: '20s' }],
};
for (const name of activeNames) {
  thresholds[`http_req_duration{api:${name}}`] = ['p(95)<500'];
  thresholds[`http_req_failed{api:${name}}`] = ['rate<0.01'];
}

export const options = {
  scenarios: { tracker: buildScenario() },
  thresholds,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const headers = {
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
};

export default function () {
  const name = pickEndpoint();
  const ep = endpoints[name];
  const params = { headers, tags: { api: name, test_type: `tracker-${mode}` } };
  const res = ep.method === 'GET'
    ? http.get(`${baseUrl}${ep.path}`, params)
    : http.request(ep.method, `${baseUrl}${ep.path}`, ep.body(), params);

  check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'status is not 5xx': (r) => r.status < 500,
  }, { api: name });
}
