# ─── Stage 1: Build ───────────────────────────────────────
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files first (layer caching)
COPY package*.json ./
RUN npm ci

# Copy source
COPY . .

# Generate Prisma client + build React frontend
RUN npx prisma generate
RUN npm run build

# ─── Stage 2: Runtime ─────────────────────────────────────
FROM node:22-alpine

# Install wget for health checks
RUN apk add --no-cache wget

# Use the existing 'node' user (UID 1000) from node:22-alpine

WORKDIR /app

# Copy everything needed from build stage (tsx requires devDeps at runtime)
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/server ./server
COPY --from=build /app/prisma ./prisma

# Create directories for persistent data
RUN mkdir -p /app/projects /app/seed && chown -R node:node /app

# Volumes for persistent data and world bundle
VOLUME ["/app/projects", "/app/seed"]

# Port
EXPOSE 3001

# Environment defaults
ENV NODE_ENV=production
ENV MAKER_PORT=3001
ENV MAKER_PROJECTS_DIR=/app/projects
ENV NEOMUD_DEFAULT_WORLD=/app/seed/default-world.nmd

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:3001/ || exit 1

# Run as non-root
USER node

# tsx runs TypeScript server directly (same as `npm run server`)
CMD ["npx", "tsx", "server/index.ts"]
