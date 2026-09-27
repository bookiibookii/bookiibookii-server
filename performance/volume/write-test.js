import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail } from 'k6';

// 쓰기 API 볼륨/동시성 테스트 (로컬 전용, write-seed.sql 데이터 기준)
//
// SCENARIO
// - create : POST /api/groups          호스트마다 1건씩 그룹 생성 (BOOK_ID로 키워드 구독자 수 조절)
// - apply  : POST /api/groups/{id}/apply 신청자마다 1건씩 참여 신청
//            GROUP_ID 지정 시 한 그룹에 집중(락 경합), GROUP_IDS_FILE 지정 시 그룹을 돌아가며 분산
// - withdraw : POST /api/users/me/withdrawal
//
// 실행 방식: RATE(req/s) × DURATION 고정 도착률. 반복마다 TOKENS_FILE의 토큰을 순서대로 하나씩 사용.

const baseUrl = (__ENV.BASE_URL || 'http://localhost:18080').replace(/\/$/, '');
const scenario = __ENV.SCENARIO || '';
const tokens = JSON.parse(open(__ENV.TOKENS_FILE));
const tokenOffset = Number(__ENV.TOKEN_OFFSET || 0);
const groupIds = __ENV.GROUP_IDS_FILE ? JSON.parse(open(__ENV.GROUP_IDS_FILE)) : [];

if (!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(baseUrl)) {
  fail('This script only targets the local volume-test server.');
}
if (!['create', 'apply', 'withdraw'].includes(scenario)) {
  fail('SCENARIO must be create, apply or withdraw');
}

export const options = {
  scenarios: {
    write: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 1),
      timeUnit: '1s',
      duration: __ENV.DURATION || '10s',
      preAllocatedVUs: Number(__ENV.PRE_VUS || 20),
      maxVUs: Number(__ENV.MAX_VUS || 300),
      gracefulStop: '120s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function isbn13(bookId) {
  return `979${String(bookId).padStart(10, '0')}`;
}

export default function () {
  const i = exec.scenario.iterationInTest;
  const t = tokens[tokenOffset + i];
  if (!t) fail(`Not enough tokens for iteration ${i}`);
  const headers = { Authorization: `Bearer ${t.token}`, 'Content-Type': 'application/json' };
  const params = { headers, tags: { api: scenario }, timeout: '120s' };
  let res;

  if (scenario === 'create') {
    res = http.post(`${baseUrl}/api/groups`, JSON.stringify({
      isbn13: isbn13(Number(__ENV.BOOK_ID || 1004)),
      readingPeriod: 14,
      groupComment: '볼륨 쓰기 테스트',
      tradeType: 'DIRECT',
      userExchangeId: t.userExchangeId,
      groupName: '같이 읽어요',
      rules: [{ tag: 'MEMO', content: null }],
    }), params);
  } else if (scenario === 'apply') {
    const groupId = __ENV.GROUP_ID || groupIds[i % groupIds.length];
    res = http.post(`${baseUrl}/api/groups/${groupId}/apply`, JSON.stringify({
      isbn13: isbn13(2000 + (i % 1000)),
      applyMsg: '같이 읽고 싶어요',
    }), params);
  } else {
    res = http.post(`${baseUrl}/api/users/me/withdrawal`, JSON.stringify({ reason: 'INFREQUENT_EXCHANGE' }), params);
  }

  check(res, { 'status is 2xx': (r) => r.status >= 200 && r.status < 300 });
  if (res.status >= 300 && i < 3) {
    console.warn(`${scenario} -> ${res.status} ${String(res.body).slice(0, 300)}`);
  }
}
