import { Navigate, useLocation } from "react-router";
import { useAuth } from "../context/useAuth.js";

export default function RequireAuth({ children }) {
  const { status } = useAuth();
  const location = useLocation();

  if (status !== "authenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
