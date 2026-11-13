
import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getOrder } from "../api/orderApi";
import { getPaymentByOrder } from "../api/paymentApi";
import { getProducts } from "../api/productApi";
import { errMsg } from "../api/axiosInstance";
import { ORDER_FLOW } from "../utils/constants";
import { dateTime, money, pretty } from "../utils/format";
import Status from "../components/Status";

const IN_PROGRESS = ["CREATED", "CONFIRMED"];

export default function OrderDetail() {
  const { id } = useParams();

  const [order, setOrder] = useState(null);
  const [payment, setPayment] = useState(null);
  const [names, setNames] = useState({});
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);

  const tries = useRef(0);

  useEffect(() => {
    getProducts()
      .then((l) =>
        setNames(Object.fromEntries(l.map((p) => [p.id, p.name])))
      )
      .catch(() => {});
  }, []);

  useEffect(() => {
    let stop = false;
    let timer;

    tries.current = 0;

    const load = async () => {
      try {
        const o = await getOrder(id);

        if (stop) return;

        setOrder(o);

        getPaymentByOrder(id)
          .then((p) => !stop && setPayment(p))
          .catch(() => {});

        // Poll briefly while the order is being processed.
        if (
          IN_PROGRESS.includes(o.status) &&
          tries.current++ < 15
        ) {
          timer = setTimeout(load, 2500);
        }
      } catch (e) {
        if (!stop) {
          setError(
            e.response?.status === 403
              ? "You don’t have access to this order."
              : e.response?.status === 404
              ? "Order not found."
              : errMsg(e)
          );
        }
      }
    };

    load();

    return () => {
      stop = true;
      clearTimeout(timer);
    };
  }, [id]);

  const refreshPayment = async () => {
    setRefreshing(true);

    try {
      const latestPayment = await getPaymentByOrder(id);
      setPayment(latestPayment);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setRefreshing(false);
    }
  };

  if (error) {
    return (
      <div className="empty">
        <h2>{error}</h2>
        <Link className="btn" to="/orders">
          My orders
        </Link>
      </div>
    );
  }

  if (!order) return <div className="skeleton tall" />;

  const cancelled = order.status === "CANCELLED";
  const idx = ORDER_FLOW.indexOf(order.status);

  return (
    <>
      <p className="crumbs">
        <Link to="/orders">My orders</Link> / #{order.id}
      </p>

      <div className="row between wrap">
        <h1>Order #{order.id}</h1>
        <Status value={order.status} />
      </div>

      <p className="muted">
        Placed on {dateTime(order.createdAt)}
      </p>

      <section className="panel">
        {cancelled ? (
          <div className="alert">
            This order was cancelled.
          </div>
        ) : (
          <ol className="steps">
            {ORDER_FLOW.map((s, i) => (
              <li
                key={s}
                className={
                  i < idx
                    ? "done"
                    : i === idx
                    ? "now"
                    : ""
                }
              >
                <span className="dot">
                  {i < idx ? "✓" : ""}
                </span>
                <span>{pretty(s)}</span>
              </li>
            ))}
          </ol>
        )}

        {IN_PROGRESS.includes(order.status) && (
          <p className="muted small">
            Confirming your order. Payment is due in cash
            when your order is delivered.
          </p>
        )}
      </section>

      <div className="two-col">
        <section className="panel">
          <h3>Items</h3>

          {order.items?.map((i) => (
            <div
              key={i.id ?? i.productId}
              className="row between rline"
            >
              <Link to={`/product/${i.productId}`}>
                {names[i.productId] || `Product #${i.productId}`}
                <span className="muted">
                  {" "}× {i.quantity}
                </span>
              </Link>

              <span>
                {money(i.price * i.quantity)}
              </span>
            </div>
          ))}

          <hr />

          <div className="row between total">
            <span>Total</span>
            <span>{money(order.totalAmount)}</span>
          </div>
        </section>

        <section className="panel">
          <div className="row between">
            <h3>Payment</h3>

            <button
              className="btn sm"
              onClick={refreshPayment}
              disabled={refreshing}
            >
              {refreshing ? "Refreshing..." : "Refresh status"}
            </button>
          </div>

          {payment ? (
            <>
              <div className="row between rline">
                <span className="muted">Status</span>
                <Status value={payment.status} />
              </div>

              <div className="row between rline">
                <span className="muted">Method</span>
                <span>{pretty(payment.paymentMethod)}</span>
              </div>

              <div className="row between rline">
                <span className="muted">Amount</span>
                <span>{money(payment.amount)}</span>
              </div>

              <div className="row between rline">
                <span className="muted">Date</span>
                <span>{dateTime(payment.createdAt)}</span>
              </div>

              {payment.paymentMethod === "COD" &&
                payment.status === "PENDING" && (
                  <p className="muted small">
                    Please pay in cash when your order is delivered.
                  </p>
                )}

              {payment.paymentMethod === "COD" &&
                payment.status === "SUCCESS" && (
                  <p className="muted small">
                    Cash payment has been collected successfully.
                  </p>
                )}
            </>
          ) : (
            <p className="muted">
              Your COD payment details are being prepared.
              Please refresh shortly.
            </p>
          )}
        </section>
      </div>
    </>
  );
}
