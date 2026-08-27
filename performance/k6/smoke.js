import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const token = __ENV.TOKEN || '';
const path = __ENV.PATH || '';
const vus = Number(__ENV.VUS || 5);

if (__ENV.ALLOW_LOAD_TEST !== 'true') {
  fail('Refusing to run: set ALLOW_LOAD_TEST=true only after confirming BASE_URL is DEV.');
}
if (!/^https?:\/\//.test(baseUrl)) {
  fail('BASE_URL must be an absolute http(s) URL.');
}
if (!path) {
  fail('PATH is required. Set it to a safe DEV GET API path.');
}
if (!path.startsWith('/') || /[?&](delete|mutate|write)=/i.test(path)) {
  fail('PATH must be a safe GET path beginning with /.');
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

export default function () {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const response = http.get(`${baseUrl}${path}`, {
    headers,
    tags: { test_type: 'dev-read-only' },
  });

  check(response, {
    'status is not 5xx': (result) => result.status < 500,
  });
  sleep(1);
}
