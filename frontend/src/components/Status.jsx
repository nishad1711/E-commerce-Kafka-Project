import { pretty } from "../utils/format";
const TONE = { CREATED: "grey", CONFIRMED: "blue", PAID: "green", SHIPPED: "violet", DELIVERED: "green", CANCELLED: "red", PENDING: "amber", SUCCESS: "green", FAILED: "red", REFUNDED: "grey" };
export default function Status({ value }) {
  return <span className={`pill ${TONE[value] || "grey"}`}>{pretty(value)}</span>;
}
