import type {
  ApiErrorBody,
  CompareResponse,
  SimulationRequest,
  SimulationResponse,
  WorkloadInfo,
} from '../types/simulation';

/** Base URL of the Spring Boot API. Configure with VITE_API_BASE_URL; never hard-code it in components. */
export const API_BASE_URL: string = (
  (import.meta.env?.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api'
).replace(/\/+$/, '');

/** A failure that is safe to show to the user (never contains a stack trace). */
export class ApiError extends Error {
  readonly status: number | null;
  readonly kind: 'network' | 'validation' | 'server' | 'not-found';

  constructor(message: string, kind: ApiError['kind'], status: number | null = null) {
    super(message);
    this.name = 'ApiError';
    this.kind = kind;
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...(init?.headers ?? {}) },
    });
  } catch {
    throw new ApiError(
      `Cannot reach the AdaptiveOS backend at ${API_BASE_URL}. Make sure the Spring Boot server is running.`,
      'network',
    );
  }

  if (!response.ok) {
    let message = `The server returned an error (HTTP ${response.status}).`;
    try {
      const body = (await response.json()) as Partial<ApiErrorBody>;
      if (typeof body.message === 'string' && body.message.length > 0) message = body.message;
    } catch {
      /* body was not JSON: keep the generic message */
    }
    const kind = response.status === 404 ? 'not-found' : response.status >= 500 ? 'server' : 'validation';
    throw new ApiError(message, kind, response.status);
  }

  try {
    return (await response.json()) as T;
  } catch {
    throw new ApiError('The server sent a response that could not be read.', 'server', response.status);
  }
}

export const api = {
  simulate: (body: SimulationRequest) =>
    request<SimulationResponse>('/simulate', { method: 'POST', body: JSON.stringify(body) }),
  compare: (body: Omit<SimulationRequest, 'mode'>) =>
    request<CompareResponse>('/compare', { method: 'POST', body: JSON.stringify(body) }),
  workloads: () => request<WorkloadInfo[]>('/workloads'),
  workload: (id: string) => request<WorkloadInfo>(`/workloads/${encodeURIComponent(id)}`),
  health: () => request<{ status: string }>('/health'),
};
