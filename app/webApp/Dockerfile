# syntax=docker/dockerfile:1
# Build context: the repository root (docker-compose.yml sets it).

# The build runs on the machine that builds (its output does not depend on the CPU), so a multi-platform image
# needs no emulated Kotlin build; only the final stage is made for each target platform.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
# the pnpm binary the wasm build downloads needs libatomic
RUN apt-get update && apt-get install -y --no-install-recommends libatomic1 && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
# The cache mount keeps the Kotlin toolchain and the downloaded dependencies between builds.
RUN --mount=type=cache,target=/root/.cache/JetBrains \
    ./kotlin build -m webApp -p wasmJs -v release

FROM nginx:1.29-alpine
COPY app/webApp/docker/nginx.conf /etc/nginx/conf.d/default.conf
# nginx runs the scripts in this folder before it starts
COPY --chmod=755 app/webApp/docker/40-write-config.sh /docker-entrypoint.d/40-write-config.sh
COPY --from=build /src/build/tasks/_webApp_buildWasmJsAppWasmJsRelease/ /usr/share/nginx/html/
EXPOSE 80
