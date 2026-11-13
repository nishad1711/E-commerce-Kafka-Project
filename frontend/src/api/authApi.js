import api from "./axiosInstance";

export const login = async (email, password) => {
  const { data } = await api.post("/api/users/login", {
    email,
    password
  });

  const token = data?.token;

  if (!token) {
    throw new Error("Login response did not contain a JWT token.");
  }

  return token;
};

export const register = async ({ name, email, password }) => {
  const { data } = await api.post("/api/users/register", {
    name,
    email,
    password
  });

  return data;
};
