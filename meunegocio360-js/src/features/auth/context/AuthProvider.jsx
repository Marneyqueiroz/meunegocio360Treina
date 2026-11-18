import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useLocation, useNavigate } from "react-router";
import { authApi } from "../api/authApi.js";
import { configureHttpAuth } from "../../../shared/lib/http.js";
import { AuthContext } from "./authContext.js";

function internalPath(value) {
  if (
    typeof value !== "string" ||
    !value.startsWith("/") ||
    value.startsWith("//")
  )
    return "/";
  return value;
}

export default function AuthProvider({ children, queryClient }) {
  const navigate = useNavigate();
  const location = useLocation();
  const accessTokenRef = useRef(null);
  const expiredRef = useRef(false);
  const [session, setSession] = useState({
    status: "loading",
    user: null,
    expiresIn: 0,
  });

  const applySession = useCallback((value) => {
    accessTokenRef.current = value.accessToken;
    expiredRef.current = false;
    setSession({
      status: "authenticated",
      user: value.usuario,
      expiresIn: value.expiresIn,
    });
  }, []);

  const endExpiredSession = useCallback(() => {
    if (expiredRef.current) return;
    expiredRef.current = true;
    accessTokenRef.current = null;
    setSession({ status: "anonymous", user: null, expiresIn: 0 });
    queryClient.clear();

    const current = internalPath(
      `${location.pathname}${location.search}${location.hash}`,
    );
    navigate("/login", {
      replace: true,
      state: { from: current, sessionExpired: true },
    });
  }, [
    location.hash,
    location.pathname,
    location.search,
    navigate,
    queryClient,
  ]);

  useEffect(() => {
    configureHttpAuth({
      getAccessToken: () => accessTokenRef.current,
      onSessionUpdated: applySession,
      onSessionExpired: endExpiredSession,
    });
  }, [applySession, endExpiredSession]);

  const restore = useCallback(async () => {
    try {
      const value = await authApi.refresh();
      applySession(value);
    } catch (error) {
      accessTokenRef.current = null;
      if (error.status === 401) {
        setSession({ status: "anonymous", user: null, expiresIn: 0 });
      } else {
        setSession({ status: "restore-error", user: null, expiresIn: 0 });
      }
    }
  }, [applySession]);

  useEffect(() => {
    const timer = window.setTimeout(() => void restore(), 0);
    return () => window.clearTimeout(timer);
  }, [restore]);

  useEffect(() => {
    if (session.status !== "authenticated" || session.expiresIn <= 60)
      return undefined;
    const timer = window.setTimeout(
      () => {
        authApi.refresh().catch(() => {});
      },
      (session.expiresIn - 60) * 1000,
    );
    return () => window.clearTimeout(timer);
  }, [session.expiresIn, session.status]);

  const login = useCallback(
    async (credentials) => {
      const value = await authApi.login(credentials);
      queryClient.clear();
      applySession(value);
      return value;
    },
    [applySession, queryClient],
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // A falha de rede no logout não mantém a sessão no cliente.
    }
    accessTokenRef.current = null;
    expiredRef.current = false;
    setSession({ status: "anonymous", user: null, expiresIn: 0 });
    queryClient.clear();
    navigate("/login", { replace: true });
  }, [navigate, queryClient]);

  const retryRestore = useCallback(() => {
    setSession((current) => ({ ...current, status: "loading" }));
    void restore();
  }, [restore]);

  const value = useMemo(
    () => ({
      ...session,
      login,
      logout,
      retryRestore,
    }),
    [login, logout, retryRestore, session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
