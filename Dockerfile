FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg clamav-daemon clamav-freshclam && rm -rf /var/lib/apt/lists/*
RUN useradd -r -u 10001 appuser
# Run clamd on a local TCP port instead of its default unix socket, and let the app
# reach it via localhost now that it lives in the same container.
RUN { echo "TCPSocket 3310"; echo "TCPAddr 127.0.0.1"; } >> /etc/clamav/clamd.conf
ENV MALWARE_SCAN_ENABLED=true CLAMAV_HOST=127.0.0.1 CLAMAV_PORT=3310
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
