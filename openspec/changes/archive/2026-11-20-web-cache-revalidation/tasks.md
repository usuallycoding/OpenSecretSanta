# Tasks

## 1. Web & Server Cache Headers

- [x] 1.1 Update `app/webApp/docker/nginx.conf` to add `add_header Cache-Control "no-cache";` to the `.mjs` location and add a `location ~ \.(js|wasm|css)$` block with `add_header Cache-Control "no-cache";`; verify nginx config syntax/structure and existing exact locations remain intact.
- [x] 1.2 Update `server/src/presentation/HealthRoutes.kt` to set `Cache-Control: no-store` on `GET /health` responses and add a test assertion in `server/test/ApplicationTest.kt`; verify with `./kotlin test -m server --include-classes=com.usuallycoding.opensecretsanta.server.ApplicationTest`.
