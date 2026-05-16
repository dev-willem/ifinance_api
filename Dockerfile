# ---------- Build ----------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download deps — cached layer (rebuilds only on pom.xml change)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src src

RUN ./mvnw package -DskipTests -q

# ---------- Runtime ----------
FROM eclipse-temurin:25-jre-alpine AS production

RUN addgroup -S ifinance && adduser -S ifinance -G ifinance

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown ifinance:ifinance app.jar

USER ifinance

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
