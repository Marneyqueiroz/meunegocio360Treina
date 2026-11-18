const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api/v1").replace(
  /\/$/,
  "",
);
const REQUEST_TIMEOUT_MS = 12_000;

let getAccessToken = () => null;
let onSessionUpdated = () => {};
let onSessionExpired = () => {};
let refreshInFlight = null;

export class ApiError extends Error {
  constructor({ status, code, detail, errors = [], retryAfter = null, cause }) {
    super(detail || "Não foi possível concluir a solicitação.", { cause });
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.detail = detail || "Não foi possível concluir a solicitação.";
    this.errors = errors;
    this.retryAfter = retryAfter;
  }
}

export function configureHttpAuth(callbacks) {
  getAccessToken = callbacks.getAccessToken;
  onSessionUpdated = callbacks.onSessionUpdated;
  onSessionExpired = callbacks.onSessionExpired;
}

function requestId() {
  return (
    globalThis.crypto?.randomUUID?.() ||
    `${Date.now()}-${Math.random().toString(16).slice(2)}`
  );
}

async function send(path, options = {}) {
  const { method = "GET", body, signal, authenticated = true } = options;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const headers = new Headers(options.headers);
  headers.set("X-Request-Id", requestId());

  if (body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (authenticated && getAccessToken()) {
    headers.set("Authorization", `Bearer ${getAccessToken()}`);
  }

  const forwardAbort = () => controller.abort();
  signal?.addEventListener("abort", forwardAbort, { once: true });

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body:
        body === undefined
          ? undefined
          : typeof body === "string"
            ? body
            : JSON.stringify(body),
      credentials: "same-origin",
      signal: controller.signal,
    });
    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = null;
      }
    }

    if (!response.ok) {
      throw new ApiError({
        status: response.status,
        code: data?.code || "HTTP_ERROR",
        detail: data?.detail,
        errors: Array.isArray(data?.errors) ? data.errors : [],
        retryAfter: Number(response.headers.get("Retry-After")) || null,
      });
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (controller.signal.aborted) {
      throw new ApiError({
        status: 0,
        code: "REQUEST_TIMEOUT",
        detail: "A solicitação demorou demais. Tente novamente.",
        cause: error,
      });
    }
    throw new ApiError({
      status: 0,
      code: "NETWORK_ERROR",
      detail: "Não foi possível conectar ao serviço.",
      cause: error,
    });
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener("abort", forwardAbort);
  }
}

export async function refreshSession() {
  if (!refreshInFlight) {
    refreshInFlight = send("/auth/refresh", {
      method: "POST",
      authenticated: false,
    })
      .then((session) => {
        onSessionUpdated(session);
        return session;
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

export async function request(path, options = {}) {
  const { authenticated = true, retryUnauthorized = true } = options;
  try {
    return await send(path, { ...options, authenticated });
  } catch (error) {
    if (
      error.status !== 401 ||
      !authenticated ||
      !retryUnauthorized ||
      path === "/auth/refresh"
    ) {
      throw error;
    }

    try {
      await refreshSession();
    } catch (refreshError) {
      onSessionExpired();
      throw refreshError;
    }

    return send(path, { ...options, authenticated, retryUnauthorized: false });
  }
}
