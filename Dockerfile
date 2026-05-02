FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && \
    ./mvnw dependency:resolve -B --no-transfer-progress

COPY src src
RUN ./mvnw package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar
COPY start.sh ./
RUN chmod +x ./start.sh

ENTRYPOINT ["./start.sh"]
