# ==============================================================================
# Frontend Dockerfile — Multi-stage Build for React / Vite SPA
# ==============================================================================

# Stage 1: Build React SPA
FROM node:22-alpine AS builder
WORKDIR /app

# Cache package dependencies layer
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build production assets
COPY . .
RUN npm run build

# Stage 2: Serve with Production-Hardened Nginx
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Configure Asia/Ho_Chi_Minh timezone
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone \
    && apk del tzdata

# Remove default Nginx website and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
