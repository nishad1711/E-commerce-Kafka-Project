import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { TOKEN_KEY } from "../api/axiosInstance";
import * as authApi from "../api/authApi";
import { decodeJwt } from "../utils/format";
import { useToast } from "./ToastContext";

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

const readToken = () => {
  const t = localStorage.getItem(TOKEN_KEY);
  const p = t && decodeJwt(t);
  if (!p || (p.exp && p.exp * 1000 < Date.now())) {
    localStorage.removeItem(TOKEN_KEY);
    return null;
  }
  return t;
};

export function AuthProvider({ children }) {
  const toast = useToast();
  const [token, setToken] = useState(readToken);

  const user = useMemo(() => {
    const p = token && decodeJwt(token);
    return p ? { email: p.sub, userId: p.userId, role: p.role, exp: p.exp } : null;
  }, [token]);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
  }, []);

  const login = useCallback(async (email, password) => {
    const t = await authApi.login(email, password);
    localStorage.setItem(TOKEN_KEY, t);
    setToken(t);
  }, []);

  useEffect(() => {
    const onExpired = () => {
      setToken(null);
      toast("Your session expired. Please sign in again.", "err");
    };
    window.addEventListener("auth:expired", onExpired);
    return () => window.removeEventListener("auth:expired", onExpired);
  }, [toast]);

  useEffect(() => {
    if (!user?.exp) return;
    const ms = user.exp * 1000 - Date.now();
    const id = setTimeout(() => { logout(); toast("Your session expired. Please sign in again.", "err"); }, Math.max(ms, 0));
    return () => clearTimeout(id);
  }, [user, logout, toast]);

  const value = { token, user, isAdmin: user?.role === "ADMIN", login, logout, register: authApi.register };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
