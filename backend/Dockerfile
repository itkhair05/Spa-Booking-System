# ==============================================================================
# Backend Dockerfile — Multi-stage Build for Spring Boot Application
# ==============================================================================

# Stage 1: Build application JAR
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Configure Asia/Ho_Chi_Minh timezone, utility tools, and su-exec for safe non-root privilege dropping
RUN apk add --no-cache tzdata curl su-exec \
    && cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone \
    && apk del tzdata

# Create dedicated non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create upload mount directory and assign permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy artifact from build stage
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appgroup /app/app.jar

# Copy entrypoint script, strip potential CRLF line breaks, and set executable permission
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh

EXPOSE 8080

ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Ho_Chi_Minh -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["/app/docker-entrypoint.sh"]
