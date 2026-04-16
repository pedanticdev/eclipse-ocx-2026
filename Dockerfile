FROM eclipse-temurin:25 AS build

WORKDIR /app

COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre

ARG PAYARA_VERSION=7.2026.4

ADD https://nexus.payara.fish/repository/payara-community/fish/payara/extras/payara-micro/${PAYARA_VERSION}/payara-micro-${PAYARA_VERSION}.jar /opt/payara/payara-micro.jar

COPY --from=build /app/target/ocx.war /opt/payara/deployments/ROOT.war

# Jlama model cache directory (model downloaded on first use)
RUN mkdir -p /opt/jlama/models

EXPOSE 8080

ENTRYPOINT ["java", \
    "--add-modules=jdk.incubator.vector", \
    "-jar", "/opt/payara/payara-micro.jar", \
    "--deploy", "/opt/payara/deployments/ROOT.war", \
    "--nocluster"]
