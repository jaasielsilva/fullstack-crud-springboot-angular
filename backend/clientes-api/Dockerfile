# syntax=docker/dockerfile:1.7
# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy only files needed for dependency resolution first (cache layer)
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
RUN mvn -B -e -ntp dependency:go-offline

# Now copy sources and build
COPY src ./src
RUN mvn -B -e -ntp clean package -DskipTests \
    && mkdir -p target/extracted \
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:17-jre-jammy

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app && chown -R spring:spring /app

# Install curl for healthcheck (small footprint)
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
USER spring:spring

# Copy layered jars from build stage (better Docker cache reuse)
ARG EXTRACTED=/workspace/target/extracted
COPY --from=build --chown=spring:spring ${EXTRACTED}/dependencies/ ./
COPY --from=build --chown=spring:spring ${EXTRACTED}/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring ${EXTRACTED}/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring ${EXTRACTED}/application/ ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" >/dev/null || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
