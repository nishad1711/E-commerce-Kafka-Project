import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getOrdersByUser } from "../api/orderApi";
import { errMsg } from "../api/axiosInstance";
import { useAuth } from "../context/AuthContext";
import { dateTime, money } from "../utils/format";
import Status from "../components/Status";

export default function Orders() {
  const { user } = useAuth();
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getOrdersByUser(user.userId)
      .then((o) => setOrders([...o].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))))
      .catch((e) => setError(errMsg(e)));
  }, [user.userId]);

  return (
    <>
      <h1>My orders</h1>
      {error && <div className="alert">{error}</div>}
      {!orders && !error && <div className="stack">{[1, 2, 3].map((i) => <div key={i} className="skeleton row-sk" />)}</div>}
      {orders?.length === 0 && <div className="empty"><h3>No orders yet</h3><p>When you place an order, it will appear here.</p><Link className="btn" to="/">Start shopping</Link></div>}
      <div className="stack">
        {orders?.map((o) => (
          <Link key={o.id} to={`/orders/${o.id}`} className="panel order-row">
            <div>
              <strong>Order #{o.id}</strong>
              <div className="muted small">{dateTime(o.createdAt)} · {o.items?.length || 0} {o.items?.length === 1 ? "item" : "items"}</div>
            </div>
            <Status value={o.status} />
            <strong>{money(o.totalAmount)}</strong>
          </Link>
        ))}
      </div>
    </>
  );
}
