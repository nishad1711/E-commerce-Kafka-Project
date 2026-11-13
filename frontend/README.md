# Cartly — React storefront for your microservices backend

Vite + React 18 + React Router v6 + Axios. All calls go through the API Gateway (`http://localhost:8080`).

## Run
```
npm install
npm run dev        # http://localhost:5173
```
Change the gateway URL with `VITE_API_BASE_URL` (see `.env.example`).

## Backend must allow
- CORS for `http://localhost:5173` on the gateway (methods GET/POST/PUT/DELETE, header `Authorization`).

## Things NOT in your API contract (edit in `src/api/authApi.js`)
- `POST /api/auth/login`  body `{email,password}` -> `{token}` (also accepts accessToken / jwt / raw string)
- `POST /api/auth/register` body `{name,email,password}`
Replace the paths/fields with your real auth endpoints.

## What's included
Home (search, category chips, sort, in-stock filter) · Product detail · Cart (stock-capped) · Checkout ·
Order history · Order detail with live status timeline (auto-polls while CREATED/CONFIRMED) + payment info ·
Login/Register · Admin: product CRUD, order lookup + status update.

React never calls internal endpoints (`POST /api/payments`, `PUT /api/payments/{id}/status`, reserve/release stock).
Razorpay is intentionally left for later, once the backend exposes its public endpoints.
