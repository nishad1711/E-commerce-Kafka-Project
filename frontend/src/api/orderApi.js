import api from "./axiosInstance";

// userId is deliberately omitted: the backend takes it from the JWT.
export const createOrder = (items) =>
  api.post("/api/orders", { items: items.map((i) => ({ productId: i.id, quantity: i.quantity })) }).then((r) => r.data);
export const getOrder = (id) => api.get(`/api/orders/${id}`).then((r) => r.data);
export const getOrdersByUser = (userId) => api.get(`/api/orders/user/${userId}`).then((r) => r.data);
// ADMIN only
export const updateOrderStatus = (id, status) =>
  api.put(`/api/orders/${id}/status`, null, { params: { status } }).then((r) => r.data);
