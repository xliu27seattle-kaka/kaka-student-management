# ---- dependencies and sources ----
FROM node:24-alpine AS base
WORKDIR /app
ENV NG_CLI_ANALYTICS=false CI=true
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .

# ---- formatting, lint and unit tests (scripts/test.sh frontend), the same checks as CI ----
FROM base AS test
RUN npx prettier --check "src/**/*.{ts,html,scss}"
RUN npx eslint "src/**/*.ts" "src/**/*.html"
RUN npx ng test --watch=false

# ---- production build ----
FROM base AS build
RUN npx ng build --configuration production

# ---- serve it with nginx, which also forwards /api to the backend ----
FROM nginx:1.29-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
