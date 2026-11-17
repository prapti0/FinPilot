const API = '/api/v1';

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('finpilot_token');
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API}${path}`, {
    ...init,
    headers: { ...authHeaders(), ...(init?.headers || {}) },
  });
  const text = await res.text();

  if (!res.ok) {
    if (res.status === 502 || res.status === 503 || res.status === 504) {
      throw new Error('Backend offline — run: bash FinPilot/scripts/start-local.sh');
    }
    if (!text.trim()) {
      throw new Error(`Request failed (${res.status} ${res.statusText}). Is gateway on :8080?`);
    }
    try {
      const err = JSON.parse(text) as { message?: string; error?: string };
      throw new Error(err.message || err.error || text);
    } catch (e) {
      if (e instanceof SyntaxError) {
        throw new Error(text || res.statusText);
      }
      throw e;
    }
  }

  if (!text.trim()) {
    throw new Error('Empty response from server');
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    throw new Error('Invalid JSON from server');
  }
}

export type AuthResponse = {
  accessToken: string;
  userId: number;
  email: string;
  name: string;
  role: string;
};

export async function login(email: string, password: string) {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

export async function register(name: string, email: string, password: string) {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ name, email, password }),
  });
}

export type Balance = { available: number; reserved: number; totalCash: number };

export async function getBalance() {
  return request<Balance>('/accounts/me/balance');
}

export type PortfolioSummary = {
  portfolioId: number;
  holdingCount: number;
  investedValue: number;
  sectorWeights: Record<string, number>;
};

export type PortfolioDetail = {
  id: number;
  name: string;
  summary: PortfolioSummary;
};

export async function getPortfolios() {
  return request<PortfolioSummary[]>('/portfolios');
}

export async function getPortfolioDetail(id: number) {
  return request<PortfolioDetail>(`/portfolios/${id}`);
}

export type Holding = {
  symbol: string;
  name: string;
  sector: string;
  quantity: number;
  avgPrice: number;
};

export type Performance = {
  portfolioId: number;
  totalCost: number;
  markedValue: number;
  unrealizedPnl: number;
};

export async function getHoldings(portfolioId: number) {
  return request<Holding[]>(`/portfolios/${portfolioId}/holdings`);
}

export async function getPerformance(portfolioId: number) {
  return request<Performance>(`/portfolios/${portfolioId}/performance`);
}

export type Quote = { symbol: string; price: number; asOf: string };

export async function getQuote(symbol: string) {
  return request<Quote>(`/market/quotes/${symbol}`);
}

export type OrderResponse = {
  id: number;
  status: string;
  symbol: string;
  side: string;
  quantity: number;
  limitPrice: number;
  rejectionReason?: string;
};

export async function placeOrder(
  portfolioId: number,
  symbol: string,
  side: string,
  quantity: number,
  limitPrice: number,
  idempotencyKey: string
) {
  return request<OrderResponse>('/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ portfolioId, symbol, side, quantity, limitPrice }),
  });
}

export async function analyzePortfolio(portfolioId: number, question: string) {
  return request<{ narrative: string; metadata: Record<string, unknown> }>('/ai/analyze-portfolio', {
    method: 'POST',
    body: JSON.stringify({ portfolioId, question }),
  });
}

export async function engineeringReview(prTitle: string, diff: string, repository: string) {
  return request<{
    summary: string;
    findings: { severity: string; title: string; detail: string }[];
  }>('/ai/engineering/review', {
    method: 'POST',
    body: JSON.stringify({ prTitle, diff, repository }),
  });
}

export async function getRiskAlerts() {
  return request<{ id: number; alertType: string; severity: string; payload: Record<string, unknown> }[]>(
    '/risk/alerts'
  );
}

export async function serviceHealth() {
  return request<{ name: string; status: string }[]>('/monitoring/services');
}