
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Disable Cypress binary installation during Docker build
# Cypress will only run in the dedicated cypress/included container
ENV CYPRESS_INSTALL_BINARY=0

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Stage 2: Production image with nginx
FROM nginx:alpine

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

