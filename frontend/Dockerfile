# --- build stage -----------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- runtime stage -----------------------------------------------------------
FROM nginx:alpine AS runtime

# nginx's own docker-entrypoint.d/20-envsubst-on-templates.sh renders every
# *.template file here into /etc/nginx/conf.d/ at container startup,
# substituting ${PORT} - Cloud Run sets it dynamically; docker-compose/local
# runs get the default below. Nginx's own $-prefixed runtime variables
# (like $uri, $host) are untouched since they aren't real env vars.
ENV PORT=80
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
