export default function Qty({ value, max, onChange }) {
  return (
    <div className="qty">
      <button onClick={() => onChange(value - 1)} disabled={value <= 1} aria-label="Decrease quantity">−</button>
      <span>{value}</span>
      <button onClick={() => onChange(value + 1)} disabled={value >= max} aria-label="Increase quantity">+</button>
    </div>
  );
}
