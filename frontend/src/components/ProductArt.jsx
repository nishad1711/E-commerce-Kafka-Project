import { CATEGORY_EMOJI } from "../utils/constants";

const PALETTES = [
  ["#6d5dfc", "#b6a8ff"], ["#ff7a59", "#ffc2a8"], ["#12a39a", "#9be7de"], ["#e0457b", "#ffb3cd"],
  ["#2f80ed", "#a9cdfb"], ["#f2a900", "#ffe29a"], ["#7a4cf0", "#d3c2ff"], ["#2d9d5b", "#b4eacb"],
];

// Backend has no image field, so we render a deterministic colour tile per product.
export default function ProductArt({ product, big }) {
  const [a, b] = PALETTES[Math.abs(Number(product.id) || 0) % PALETTES.length];
  const emoji = CATEGORY_EMOJI[(product.category || "").toLowerCase()];
  return (
    <div className={`art ${big ? "big" : ""}`} style={{ background: `linear-gradient(145deg, ${a}, ${b})` }} aria-hidden="true">
      <span className="art-letter">{(product.name || "?").charAt(0)}</span>
      {emoji && <span className="art-emoji">{emoji}</span>}
    </div>
  );
}
