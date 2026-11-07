// k6 run of the API token rate limit (plan 20 §6) and REST latency, against a deployed
// environment. Run by the "Load test" workflow, by hand, since it spends the token's daily budget.
//
//   K6_API_URL       the API, such as https://<app>.azurecontainerapps.io
//   K6_TOKEN         a read-scoped API token
//   K6_STUDIO_ID     the token's studio
//
// What it checks: a burst of token calls succeeds until the bucket (200 by default) is empty, then
// gets 429 with Retry-After, never 5xx. Latency of the calls that succeeded is the README number.
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const api = __ENV.K6_API_URL;
const token = __ENV.K6_TOKEN;
const studio = __ENV.K6_STUDIO_ID;

const limited = new Counter('rate_limited');
const served = new Counter('served');
const servedLatency = new Trend('served_latency', true);

export const options = {
  scenarios: {
    burst: {
      executor: 'shared-iterations',
      vus: 10,
      // Past the default burst of 200, so the limit is reached
      iterations: 300,
      maxDuration: '2m',
    },
  },
  thresholds: {
    'http_req_failed{expected_response:true}': ['rate<0.01'],
    served_latency: ['p(95)<800'],
    rate_limited: ['count>0'],
  },
};

export function setup() {
  // Wake the API from zero so the cold start isn't measured
  for (let i = 0; i < 12; i++) {
    if (http.get(`${api}/actuator/health/readiness`).status === 200) {
      return;
    }
  }
  throw new Error('The API did not become ready');
}

export default function () {
  const response = http.get(`${api}/api/v1/studios/${studio}/kilns`, {
    headers: { Authorization: `Bearer ${token}` },
    responseCallback: http.expectedStatuses(200, 429),
  });
  if (response.status === 429) {
    limited.add(1);
    check(response, { '429 says when to retry': (r) => r.headers['Retry-After'] !== undefined });
  } else {
    served.add(1);
    servedLatency.add(response.timings.duration);
    check(response, { 'served is 200': (r) => r.status === 200 });
  }
}
