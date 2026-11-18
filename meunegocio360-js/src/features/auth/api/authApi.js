import { request, refreshSession } from "../../../shared/lib/http.js";

export const authApi = {
  login(credentials) {
    return request("/auth/login", {
      method: "POST",
      body: credentials,
      authenticated: false,
      retryUnauthorized: false,
    });
  },
  refresh() {
    return refreshSession();
  },
  logout() {
    return request("/auth/logout", {
      method: "POST",
      authenticated: false,
      retryUnauthorized: false,
    });
  },
};
