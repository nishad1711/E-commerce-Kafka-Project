import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getProduct } from "../api/productApi";
import { errMsg } from "../api/axiosInstance";
import { useCart } from "../context/CartContext";
import { useToast } from "../context/ToastContext";
import { money } from "../utils/format";
import ProductArt from "../components/ProductArt";
import Qty from "../components/Qty";

export default function ProductDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const { add, items } = useCart();
  const toast = useToast();
  const [p, setP] = useState(null);
  const [error, setError] = useState("");
  const [qty, setQty] = useState(1);

  useEffect(() => {
    setP(null); setError("");
    getProduct(id).then(setP).catch((e) => setError(e.response?.status === 404 ? "This product no longer exists." : errMsg(e)));
  }, [id]);

  if (error) return <div className="empty"><h2>{error}</h2><Link className="btn" to="/">Back to shop</Link></div>;
  if (!p) return <div className="detail"><div className="skeleton tall" /><div className="skeleton tall" /></div>;

  const inCart = items.find((i) => i.id === p.id)?.quantity || 0;
  const left = p.stock - inCart;
  const out = p.stock <= 0;
  const addNow = () => {
    if (add(p, qty)) { toast(`${qty} × ${p.name} added to cart`); return true; }
    toast(out ? "This item is out of stock" : "You already have the maximum available in your cart", "err");
    return false;
  };

  return (
    <>
      <p className="crumbs"><Link to="/">Shop</Link> / {p.category} / {p.name}</p>
      <div className="detail">
        <ProductArt product={p} big />
        <div className="info">
          <span className="muted">{p.category}</span>
          <h1>{p.name}</h1>
          <div className="big-price">{money(p.price)}</div>
          <p className="muted small">Inclusive of all taxes</p>
          <p className="desc">{p.description}</p>
          <p className={out ? "stock red-t" : p.stock <= 5 ? "stock amber-t" : "stock green-t"}>
            {out ? "Currently out of stock" : p.stock <= 5 ? `Hurry, only ${p.stock} left` : `In stock · ${p.stock} available`}
          </p>
          {!out && (
            <div className="row gap wrap">
              <Qty value={Math.min(qty, Math.max(left, 1))} max={Math.max(left, 1)} onChange={setQty} />
              {left <= 0 && <span className="muted small">All available units are in your cart</span>}
            </div>
          )}
          <div className="row gap wrap actions">
            <button className="btn lg" disabled={out || left <= 0} onClick={addNow}>Add to cart</button>
            <button className="btn lg outline" disabled={out || (left <= 0 && inCart === 0)} onClick={() => { if (left <= 0 || addNow()) nav("/checkout"); }}>Buy now</button>
          </div>
          <ul className="perks">
            <li>Cash on Delivery</li><li>Live order tracking</li><li>Stock reserved at checkout</li>
          </ul>
        </div>
      </div>
    </>
  );
}
