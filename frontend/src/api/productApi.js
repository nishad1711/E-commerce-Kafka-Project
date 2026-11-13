import api from "./axiosInstance";

export const getProducts = () => api.get("/api/products").then((r) => r.data);
export const getProduct = (id) => api.get(`/api/products/${id}`).then((r) => r.data);
// ADMIN only
export const createProduct = (p) => api.post("/api/products", p).then((r) => r.data);
export const updateProduct = (id, p) => api.put(`/api/products/${id}`, p).then((r) => r.data);
export const deleteProduct = (id) => api.delete(`/api/products/${id}`);
