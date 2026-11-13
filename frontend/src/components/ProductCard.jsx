import { Link } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useToast } from "../context/ToastContext";
import { money } from "../utils/format";
import ProductArt from "./ProductArt";

export default function ProductCard({ p }) {
  const { add, items } = useCart();
  const toast = useToast();
  const inCart = items.find((i) => i.id === p.id)?.quantity || 0;
  const out = p.stock <= 0;
  const onAdd = () => (add(p, 1) ? toast(`${p.name} added to cart`) : toast(out ? "This item is out of stock" : `Only ${p.stock} available`, "err"));

  return (
    <article className="card">
      <Link to={`/product/${p.id}`} className="card-link">
        <ProductArt product={p} />
        <div className="card-body">
          <span className="muted small">{p.category}</span>
          <h3 className="clamp">{p.name}</h3>
          <div className="row between">
            <strong className="price">{money(p.price)}</strong>
            {out ? <span className="tag red">Out of stock</span> : p.stock <= 5 ? <span className="tag amber">Only {p.stock} left</span> : null}
          </div>
        </div>
      </Link>
      <button className="btn block" disabled={out || inCart >= p.stock} onClick={onAdd}>
        {out ? "Out of stock" : inCart ? `In cart (${inCart}) · Add more` : "Add to cart"}
      </button>
    </article>
  );
}
