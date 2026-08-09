# Step 1: Build Angular frontend
FROM node:22-alpine AS builder

WORKDIR /usr/src/app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN NODE_OPTIONS=--openssl-legacy-provider ./node_modules/.bin/ng build --prod

# Step 2: Serve static app using Nginx
FROM nginx:alpine
LABEL version="20.06.0"

COPY nginx.conf /etc/nginx/nginx.conf 2>/dev/null || true
COPY --from=builder /usr/src/app/dist/tower-web/ /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
