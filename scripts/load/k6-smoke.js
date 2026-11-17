import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.API_BASE || 'http://localhost:8080';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { 'health ok': (r) => r.status === 200 });

  const login = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: 'demo@finpilot.com', password: 'demo1234' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(login, { 'login ok': (r) => r.status === 200 });
  sleep(1);
}