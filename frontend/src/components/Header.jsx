import { useEffect, useState } from "react";
import { Link, NavLink, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

export default function Header() {
  const { user, isAdmin, logout } = useAuth();
  const { count } = useCart();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [q, setQ] = useState(params.get("q") || "");
  const [menu, setMenu] = useState(false);
  useEffect(() => setQ(params.get("q") || ""), [params]);

  const submit = (e) => {
    e.preventDefault();
    nav(q.trim() ? `/?q=${encodeURIComponent(q.trim())}` : "/");
  };

  return (
    <header className="header">
      <div className="container header-in">
        <Link to="/" className="logo">cartly<span>.</span></Link>
        <form className="search" onSubmit={submit} role="search">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search for products, categories…" aria-label="Search products" />
          <button aria-label="Search">Search</button>
        </form>
        <nav className="nav">
          {user && <NavLink to="/orders">Orders</NavLink>}
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
          <NavLink to="/cart" className="cart-link">Cart{count > 0 && <b className="badge">{count}</b>}</NavLink>
          {user ? (
            <div className="menu">
              <button className="avatar" onClick={() => setMenu(!menu)} aria-label="Account menu">{(user.email || "U")[0].toUpperCase()}</button>
              {menu && (
                <div className="dropdown" onMouseLeave={() => setMenu(false)}>
                  <div className="small muted">Signed in as</div>
                  <div className="ellipsis"><strong>{user.email}</strong></div>
                  <div className="small muted">{user.role}</div>
                  <hr />
                  <Link to="/orders" onClick={() => setMenu(false)}>My orders</Link>
                  <button className="linklike" onClick={() => { setMenu(false); logout(); nav("/"); }}>Sign out</button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/login" className="btn sm">Sign in</Link>
          )}
        </nav>
      </div>
    </header>
  );
}
