# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY proto/ proto/
COPY apigateway/pom.xml apigateway/
COPY auth/pom.xml auth/
COPY card/pom.xml card/
COPY email/pom.xml email/
COPY merchant/pom.xml merchant/
COPY role/pom.xml role/
COPY saldo/pom.xml saldo/
COPY topup/pom.xml topup/
COPY transaction/pom.xml transaction/
COPY transfer/pom.xml transfer/
COPY user/pom.xml user/
COPY withdraw/pom.xml withdraw/
COPY db-migration/pom.xml db-migration/
COPY common/src common/src
COPY email/src email/src

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl email -am -DskipTests -q -Dmaven.wagon.http.retryHandler.count=5

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/email/target/email-*-fat.jar app.jar
COPY chaos.yaml .
# Email service doesn't expose any ports as it's a Kafka consumer
ENTRYPOINT ["java", "-jar", "app.jar"]
