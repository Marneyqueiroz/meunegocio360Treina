import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router";
import { RequireAuth, useAuth } from "../features/auth/index.js";
import SessionStatus from "../features/auth/components/SessionStatus.jsx";

const LoginPage = lazy(() => import("../features/auth/pages/LoginPage.jsx"));
const HomePage = lazy(() => import("../features/auth/pages/HomePage.jsx"));
const NotFoundPage = lazy(
  () => import("../features/auth/pages/NotFoundPage.jsx"),
);

export default function App() {
  const { status, retryRestore } = useAuth();

  if (status === "loading") return <SessionStatus />;
  if (status === "restore-error")
    return <SessionStatus error onRetry={retryRestore} />;

  return (
    <Suspense fallback={<SessionStatus />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <HomePage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
