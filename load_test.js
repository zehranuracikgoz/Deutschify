import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,  // 10 sanal kullanıcı
  duration: '30s', // 30 saniye boyunca
};

export function setup() {
  const loginRes = http.post('https://deutschify-1.onrender.com/auth/login',
    JSON.stringify({ email: 'perftest_20260619@test.com', password: 'PerfTest1234' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const token = JSON.parse(loginRes.body).access_token;
  return { token };
}

export default function (data) {
  let res = http.get('https://deutschify-1.onrender.com/health');
  check(res, { 'health 200': (r) => r.status === 200 });

  res = http.get('https://deutschify-1.onrender.com/words/');
  check(res, { 'words 200': (r) => r.status === 200 });

  res = http.get('https://deutschify-1.onrender.com/study/queue/1', {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  check(res, { 'queue 200': (r) => r.status === 200 });
  sleep(1);
}