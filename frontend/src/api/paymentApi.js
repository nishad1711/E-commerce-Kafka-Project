import api from "./axiosInstance";

export const getPaymentByOrder = (orderId) =>
  api.get(`/api/payments/order/${orderId}`).then((r) => r.data).catch((e) => {
    if (e.response?.status === 404) return null;
    throw e;
  });

export const collectCodPayment = (orderId) =>
  api.put(`/api/payments/order/${orderId}/cod/collect`).then((r) => r.data);