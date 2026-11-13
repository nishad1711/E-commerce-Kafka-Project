import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { money } from "../utils/format";
import ProductArt from "../components/ProductArt";
import Qty from "../components/Qty";

export default function Cart() {
  const { items, setQty, remove, total, count } = useCart();
  const nav = useNavigate();

  if (!items.length)
    return <div className="empty"><h2>Your cart is empty</h2><p>Add something you like and it will show up here.</p><Link className="btn" to="/">Browse products</Link></div>;

  return (
    <>
      <h1>Your cart <span className="muted">({count} {count === 1 ? "item" : "items"})</span></h1>
      <div className="two-col">
        <div className="stack">
          {items.map((i) => (
            <div key={i.id} className="line">
              <Link to={`/product/${i.id}`}><ProductArt product={i} /></Link>
              <div className="grow">
                <Link to={`/product/${i.id}`}><h3>{i.name}</h3></Link>
                <span className="muted small">{i.category}</span>
                <div className="row gap wrap mt">
                  <Qty value={i.quantity} max={i.stock} onChange={(q) => setQty(i.id, q)} />
                  <button className="linklike" onClick={() => remove(i.id)}>Remove</button>
                </div>
                {i.quantity >= i.stock && <p className="small amber-t">Maximum available quantity</p>}
              </div>
              <strong>{money(i.price * i.quantity)}</strong>
            </div>
          ))}
        </div>
        <aside className="summary">
          <h3>Order summary</h3>
          <div className="row between"><span>Subtotal</span><span>{money(total)}</span></div>
          <div className="row between"><span>Delivery</span><span className="green-t">Free</span></div>
          <hr />
          <div className="row between total"><span>Total</span><span>{money(total)}</span></div>
          <button className="btn block lg" onClick={() => nav("/checkout")}>Proceed to checkout</button>
        </aside>
      </div>
    </>
  );
}
