import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createOrder } from "../api/orderApi";
import { errMsg } from "../api/axiosInstance";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { money } from "../utils/format";

export default function Checkout() {
  const { items, total, clear } = useCart();
  const { user } = useAuth();
  const toast = useToast();
  const nav = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  if (!items.length)
    return <div className="empty"><h2>Nothing to check out</h2><Link className="btn" to="/">Browse products</Link></div>;

  const place = async () => {
    setBusy(true); setError("");
    try {
      const order = await createOrder(items);
      clear();
      toast("Order placed successfully. Pay cash when your order arrives.");
      nav(`/orders/${order.id}`, { replace: true });
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h1>Checkout</h1>
      <div className="two-col">
        <div className="stack">
          <section className="panel">
            <h3>Account</h3>
            <p>Order will be placed for <strong>{user.email}</strong></p>
          </section>
          <section className="panel">
            <h3>Review items</h3>
            {items.map((i) => (
              <div key={i.id} className="row between rline">
                <span>{i.name} <span className="muted">× {i.quantity}</span></span>
                <span>{money(i.price * i.quantity)}</span>
              </div>
            ))}
            <Link to="/cart" className="small">Edit cart</Link>
          </section>
          <section className="panel">
            <h3>Payment</h3>
            <div className="row between">
              <strong>Cash on Delivery</strong>
              <span className="badge">COD</span>
            </div>
            <p className="muted">
              Pay in cash when your order is delivered. No online payment is required.
            </p>
          </section>
        </div>
        <aside className="summary">
          <h3>Price details</h3>
          <div className="row between"><span>Subtotal</span><span>{money(total)}</span></div>
          <div className="row between"><span>Delivery</span><span className="green-t">Free</span></div>
          <hr />
          <div className="row between total"><span>Amount payable</span><span>{money(total)}</span></div>
          {error && <div className="alert">{error}. Check your cart quantities and try again.</div>}
          <button className="btn block lg" disabled={busy} onClick={place}>{busy ? "Placing order…" : `Place order · ${money(total)}`}</button>
        </aside>
      </div>
    </>
  );
}
