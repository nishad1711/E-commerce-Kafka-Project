import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

const Ctx = createContext(null);
export const useCart = () => useContext(Ctx);
const KEY = "cartly_cart";

export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    try { return JSON.parse(localStorage.getItem(KEY)) || []; } catch { return []; }
  });
  useEffect(() => localStorage.setItem(KEY, JSON.stringify(items)), [items]);

  // returns true if something was added (false when stock prevents it)
  const add = useCallback((p, qty = 1) => {
    const found = items.find((i) => i.id === p.id);
    const have = found?.quantity || 0;
    const next = Math.min(have + qty, p.stock);
    if (next <= have) return false;
    const line = { id: p.id, name: p.name, price: p.price, stock: p.stock, category: p.category, quantity: next };
    setItems(found ? items.map((i) => (i.id === p.id ? line : i)) : [...items, line]);
    return true;
  }, [items]);

  const setQty = useCallback((id, q) =>
    setItems((cur) => cur.map((i) => (i.id === id ? { ...i, quantity: Math.max(1, Math.min(q, i.stock)) } : i))), []);
  const remove = useCallback((id) => setItems((cur) => cur.filter((i) => i.id !== id)), []);
  const clear = useCallback(() => setItems([]), []);

  const value = useMemo(() => ({
    items, add, setQty, remove, clear,
    count: items.reduce((s, i) => s + i.quantity, 0),
    total: items.reduce((s, i) => s + i.quantity * i.price, 0),
  }), [items, add, setQty, remove, clear]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
