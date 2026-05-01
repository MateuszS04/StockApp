FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

copy .mvn .mvn
copy mvnw pom.xml ./
RUN chmod +x mvnw && \
    ./mvnw dependency:resolve -B --no-transfer-progress

copy src src
RUN ./mvnw package -DskipTests -B --no-transfer-progress

from eclipse-temurin:21-jre-alpine
WORKDIR /app

Copy --from=builder /build/target/*.jar app.jar
COPY start.sh ./
RUN chmod +x ./start.sh

ENTRYPOINT ["./start.sh"]
