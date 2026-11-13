import { useEffect, useState } from "react";
import { createProduct, deleteProduct, getProducts, updateProduct } from "../api/productApi";
import { getOrder, getOrdersByUser, updateOrderStatus } from "../api/orderApi";
import { errMsg } from "../api/axiosInstance";
import { useToast } from "../context/ToastContext";
import { ORDER_STATUSES } from "../utils/constants";
import { dateTime, money } from "../utils/format";
import Status from "../components/Status";
import { getPaymentByOrder, collectCodPayment } from "../api/paymentApi";

const EMPTY = { name: "", description: "", price: "", stock: "", category: "" };

function ProductsTab() {
  const toast = useToast();
  const [list, setList] = useState(null);
  const [form, setForm] = useState(null); // null | product-like
  const [busy, setBusy] = useState(false);
  const load = () => getProducts().then(setList).catch((e) => toast(errMsg(e), "err"));
  useEffect(() => { load(); }, []);

  const save = async (e) => {
    e.preventDefault(); setBusy(true);
    const body = { name: form.name, description: form.description, price: Number(form.price), stock: Number(form.stock), category: form.category };
    try {
      form.id ? await updateProduct(form.id, body) : await createProduct(body);
      toast(form.id ? "Product updated" : "Product created");
      setForm(null); load();
    } catch (err) { toast(errMsg(err), "err"); } finally { setBusy(false); }
  };
  const del = async (p) => {
    if (!confirm(`Delete “${p.name}”? This cannot be undone.`)) return;
    try { await deleteProduct(p.id); toast("Product deleted"); load(); } catch (err) { toast(errMsg(err), "err"); }
  };
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <>
      <div className="row between"><h2>Products</h2><button className="btn" onClick={() => setForm({ ...EMPTY })}>New product</button></div>
      <div className="table-wrap">
        <table>
          <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th></th></tr></thead>
          <tbody>
            {list?.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td><td>{p.name}</td><td>{p.category}</td><td>{money(p.price)}</td>
                <td className={p.stock <= 5 ? "amber-t" : ""}>{p.stock}</td>
                <td className="right"><button className="linklike" onClick={() => setForm({ ...p })}>Edit</button> <button className="linklike danger" onClick={() => del(p)}>Delete</button></td>
              </tr>
            ))}
            {list?.length === 0 && <tr><td colSpan="6" className="center muted">No products yet</td></tr>}
          </tbody>
        </table>
      </div>

      {form && (
        <div className="modal" onClick={() => setForm(null)}>
          <form className="modal-box stack" onClick={(e) => e.stopPropagation()} onSubmit={save}>
            <h3>{form.id ? `Edit product #${form.id}` : "New product"}</h3>
            <label>Name<input required value={form.name} onChange={set("name")} /></label>
            <label>Description<textarea rows="3" value={form.description} onChange={set("description")} /></label>
            <div className="row gap">
              <label className="grow">Price (₹)<input required type="number" min="0" step="0.01" value={form.price} onChange={set("price")} /></label>
              <label className="grow">Stock<input required type="number" min="0" step="1" value={form.stock} onChange={set("stock")} /></label>
            </div>
            <label>Category<input required value={form.category} onChange={set("category")} /></label>
            <div className="row gap right-row">
              <button type="button" className="btn outline" onClick={() => setForm(null)}>Cancel</button>
              <button className="btn" disabled={busy}>{busy ? "Saving…" : "Save product"}</button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}


function OrdersTab() {
  const toast = useToast();
  const [mode, setMode] = useState("user");
  const [q, setQ] = useState("");
  const [orders, setOrders] = useState(null);
  const [payments, setPayments] = useState({});
  const [draft, setDraft] = useState({});
  const [collecting, setCollecting] = useState(null);

  const search = async (e) => {
    e.preventDefault();

    try {
      const res = mode === "user"
        ? await getOrdersByUser(q)
        : [await getOrder(q)];

      const sorted = res.sort((a, b) => b.id - a.id);
      setOrders(sorted);

      const paymentResults = await Promise.all(
        sorted.map(async (order) => {
          try {
            return [order.id, await getPaymentByOrder(order.id)];
          } catch {
            return [order.id, null];
          }
        })
      );

      setPayments(Object.fromEntries(paymentResults));

    } catch (err) {
      setOrders([]);
      setPayments({});
      toast(errMsg(err), "err");
    }
  };

  const update = async (o) => {
    const status = draft[o.id] || o.status;

    try {
      const u = await updateOrderStatus(o.id, status);

      setOrders((cur) =>
        cur.map((x) =>
          x.id === o.id
            ? { ...x, ...(u?.id ? u : { status }) }
            : x
        )
      );

      toast(`Order #${o.id} marked ${status.toLowerCase()}`);
    } catch (err) {
      toast(errMsg(err), "err");
    }
  };

  const collectCash = async (orderId) => {
    setCollecting(orderId);

    try {
      const payment = await collectCodPayment(orderId);

      setPayments((cur) => ({
        ...cur,
        [orderId]: payment
      }));

      toast(`Cash collection confirmed for order #${orderId}`);
    } catch (err) {
      toast(errMsg(err), "err");
    } finally {
      setCollecting(null);
    }
  };

  return (
    <>
      <h2>Orders</h2>

      <form className="row gap wrap" onSubmit={search}>
        <select value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="user">Find by user ID</option>
          <option value="order">Find by order ID</option>
        </select>

        <input
          required
          type="number"
          min="1"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={mode === "user" ? "User ID" : "Order ID"}
        />

        <button className="btn">Search</button>
      </form>

      {orders?.length === 0 && (
        <p className="muted">No orders found.</p>
      )}

      {orders?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Order</th>
                <th>User</th>
                <th>Placed</th>
                <th>Total</th>
                <th>Order status</th>
                <th>Payment</th>
                <th>Change status</th>
                <th>Cash collection</th>
              </tr>
            </thead>

            <tbody>
              {orders.map((o) => {
                const payment = payments[o.id];

                return (
                  <tr key={o.id}>
                    <td>#{o.id}</td>
                    <td>{o.userId}</td>
                    <td>{dateTime(o.createdAt)}</td>
                    <td>{money(o.totalAmount)}</td>

                    <td>
                      <Status value={o.status} />
                    </td>

                    <td>
                      {payment ? (
                        <>
                          <div>{payment.paymentMethod}</div>
                          <Status value={payment.status} />
                        </>
                      ) : (
                        <span className="muted">Not available</span>
                      )}
                    </td>

                    <td>
                      <div className="row gap">
                        <select
                          value={draft[o.id] || o.status}
                          onChange={(e) =>
                            setDraft({
                              ...draft,
                              [o.id]: e.target.value
                            })
                          }
                        >
                          {ORDER_STATUSES.map((s) => (
                            <option key={s}>{s}</option>
                          ))}
                        </select>

                        <button
                          className="btn sm"
                          disabled={(draft[o.id] || o.status) === o.status}
                          onClick={() => update(o)}
                        >
                          Update
                        </button>
                      </div>
                    </td>

                    <td>
                      {payment?.paymentMethod === "COD"
                        && payment.status === "PENDING"
                        && o.status === "DELIVERED" ? (
                          <button
                            className="btn sm"
                            disabled={collecting === o.id}
                            onClick={() => collectCash(o.id)}
                          >
                            {collecting === o.id
                              ? "Processing…"
                              : "Collect cash"}
                          </button>
                        ) : payment?.status === "SUCCESS" ? (
                          <span className="green-t">Collected</span>
                        ) : (
                          <span className="muted">Not ready</span>
                        )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

export default function Admin() {
  const [tab, setTab] = useState("products");
  return (
    <>
      <h1>Admin</h1>
      <div className="chips">
        <button className={`chip ${tab === "products" ? "on" : ""}`} onClick={() => setTab("products")}>Products</button>
        <button className={`chip ${tab === "orders" ? "on" : ""}`} onClick={() => setTab("orders")}>Orders</button>
      </div>
      {tab === "products" ? <ProductsTab /> : <OrdersTab />}
    </>
  );
}
