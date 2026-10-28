# syntax=docker/dockerfile:1
# Build context: the repository root (docker-compose.yml sets it).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# The cache mount keeps the Kotlin toolchain and the downloaded dependencies between builds.
RUN --mount=type=cache,target=/root/.cache/JetBrains \
    ./kotlin package -m server

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --home-dir /app --shell /usr/sbin/nologin santa \
    && mkdir -p /app /data \
    && chown santa /data
COPY --from=build /src/build/tasks/_server_executableJarJvm/server-jvm-executable.jar /app/server.jar
USER santa
WORKDIR /app
ENV PORT=8080 \
    DB_PATH=/data/opensecretsanta.db
VOLUME /data
EXPOSE 8080
# the image has no curl or wget, so bash makes the request itself: healthy only on a 200 answer
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /health HTTP/1.0\\r\\n\\r\\n' >&3 && head -n 1 <&3 | grep -q ' 200 '"]
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
