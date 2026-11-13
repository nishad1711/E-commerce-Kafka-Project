import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { getProducts } from "../api/productApi";
import { errMsg } from "../api/axiosInstance";
import ProductCard from "../components/ProductCard";

export default function Home() {
  const [params] = useSearchParams();
  const q = (params.get("q") || "").toLowerCase();
  const [products, setProducts] = useState(null);
  const [error, setError] = useState("");
  const [cat, setCat] = useState("All");
  const [sort, setSort] = useState("featured");
  const [inStock, setInStock] = useState(false);

  const load = () => {
    setError(""); setProducts(null);
    getProducts().then(setProducts).catch((e) => setError(errMsg(e)));
  };
  useEffect(load, []);

  const categories = useMemo(() => ["All", ...new Set((products || []).map((p) => p.category).filter(Boolean))], [products]);

  const list = useMemo(() => {
    let l = (products || []).filter((p) =>
      (cat === "All" || p.category === cat) &&
      (!inStock || p.stock > 0) &&
      (!q || `${p.name} ${p.description} ${p.category}`.toLowerCase().includes(q)));
    if (sort === "low") l = [...l].sort((a, b) => a.price - b.price);
    if (sort === "high") l = [...l].sort((a, b) => b.price - a.price);
    if (sort === "new") l = [...l].sort((a, b) => b.id - a.id);
    return l;
  }, [products, cat, sort, inStock, q]);

  return (
    <>
      {!q && cat === "All" && (
        <section className="hero">
          <div>
            <h1>Everything you need.<br />Delivered to your door.</h1>
            <p>Shop {products ? `${products.length}+ products` : "thousands of products"} with live stock,Cash on Delivery and real-time order tracking.</p>
            <a href="#products" className="btn light">Start shopping</a>
          </div>
          <div className="hero-art">
            {(products || []).slice(0, 3).map((p, i) => (
              <div key={p.id} className={`hero-tile t${i}`}>{p.name}</div>
            ))}
          </div>
        </section>
      )}

      <section id="products">
        <div className="chips" role="tablist">
          {categories.map((c) => (
            <button key={c} className={`chip ${c === cat ? "on" : ""}`} onClick={() => setCat(c)}>{c}</button>
          ))}
        </div>
        <div className="row between toolbar">
          <h2>{q ? `Results for “${params.get("q")}”` : cat === "All" ? "All products" : cat}{products && <span className="muted"> · {list.length}</span>}</h2>
          <div className="row gap">
            <label className="check"><input type="checkbox" checked={inStock} onChange={(e) => setInStock(e.target.checked)} /> In stock only</label>
            <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort products">
              <option value="featured">Featured</option>
              <option value="new">Newest</option>
              <option value="low">Price: low to high</option>
              <option value="high">Price: high to low</option>
            </select>
          </div>
        </div>

        {error && <div className="alert">We couldn’t load products. {error} <button className="linklike" onClick={load}>Try again</button></div>}
        {!products && !error && <div className="grid">{Array.from({ length: 8 }).map((_, i) => <div key={i} className="card skeleton" />)}</div>}
        {products && list.length === 0 && <div className="empty"><h3>No products found</h3><p>Try a different search or clear the filters.</p></div>}
        {products && <div className="grid">{list.map((p) => <ProductCard key={p.id} p={p} />)}</div>}
      </section>
    </>
  );
}
