import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { errMsg } from "../api/axiosInstance";

export default function Auth({ mode }) {
  const isLogin = mode === "login";
  const { login, register } = useAuth();
  const toast = useToast();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ name: "", email: "", password: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError("");
    try {
      if (!isLogin) await register(f);
      await login(f.email, f.password);
      toast(isLogin ? "Welcome back!" : "Account created. You’re signed in.");
      nav(loc.state?.from || "/", { replace: true });
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth panel">
      <h1>{isLogin ? "Sign in" : "Create your account"}</h1>
      <p className="muted">{isLogin ? "Access your orders and checkout faster." : "Takes less than a minute."}</p>
      <form onSubmit={submit} className="stack">
        {!isLogin && <label>Full name<input required value={f.name} onChange={set("name")} autoComplete="name" /></label>}
        <label>Email<input required type="email" value={f.email} onChange={set("email")} autoComplete="email" /></label>
        <label>Password<input required type="password" minLength={6} value={f.password} onChange={set("password")} autoComplete={isLogin ? "current-password" : "new-password"} /></label>
        {error && <div className="alert">{error}</div>}
        <button className="btn block lg" disabled={busy}>{busy ? "Please wait…" : isLogin ? "Sign in" : "Create account"}</button>
      </form>
      <p className="center small">
        {isLogin ? <>New to Cartly? <Link to="/register" state={loc.state}>Create an account</Link></> : <>Already have an account? <Link to="/login" state={loc.state}>Sign in</Link></>}
      </p>
    </div>
  );
}
