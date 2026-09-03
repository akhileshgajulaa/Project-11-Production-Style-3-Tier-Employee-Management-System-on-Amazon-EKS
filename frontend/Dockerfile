# ---------------------------------------------------------------------------
# Stage 1 — Build the React (Vite) production bundle
# ---------------------------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---------------------------------------------------------------------------
# Stage 2 — Serve the static bundle with Nginx
# ---------------------------------------------------------------------------
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
