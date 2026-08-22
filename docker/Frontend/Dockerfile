# ---------- Stage 1: build Angular ----------
FROM node:20-alpine AS build

WORKDIR /app

# Angular package files
COPY telemetry-dashboard/package.json telemetry-dashboard/package-lock.json ./

RUN npm ci

# Angular source
COPY telemetry-dashboard/ .

# Production build
RUN npm run build -- --configuration production


# ---------- Stage 2: Nginx ----------
FROM nginx:1.27-alpine

# Nginx configuration
COPY docker/Frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Angular production files
COPY --from=build /app/dist/telemetry-dashboard /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]