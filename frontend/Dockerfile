# --- build stage -----------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite inlines env vars into the JS bundle at build time, not runtime - so
# this has to be a build arg, not something set later on the running
# container. Unset/empty (docker-compose, local builds) falls back to the
# same-origin "/api" default in src/api/client.ts.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# --- runtime stage -----------------------------------------------------------
FROM nginx:alpine AS runtime

# nginx's own docker-entrypoint.d/20-envsubst-on-templates.sh renders every
# *.template file here into /etc/nginx/conf.d/ at container startup,
# substituting ${PORT} - Cloud Run sets it dynamically; docker-compose/local
# runs get the default below. Nginx's own $-prefixed runtime variables
# (like $uri, $host) are untouched since they aren't real env vars.
ENV PORT=80
# docker-compose's service name for the backend; a deployment where this
# nginx never actually proxies "/api/" itself (Cloud Run) should override
# this to any host:port nginx can resolve without a real DNS lookup (an IP
# literal, e.g. 127.0.0.1:1) - see nginx.conf.template for why it still
# needs to be resolvable even when unused.
ENV BACKEND_UPSTREAM=backend:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
