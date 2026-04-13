FROM eclipse-temurin:21-jre

ARG PAYARA_VERSION=7.2026.3

ADD https://nexus.payara.fish/repository/payara-community/fish/payara/extras/payara-micro/${PAYARA_VERSION}/payara-micro-${PAYARA_VERSION}.jar /opt/payara/payara-micro.jar

COPY target/ocx.war /opt/payara/deployments/ROOT.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/payara/payara-micro.jar", "--deploy", "/opt/payara/deployments/ROOT.war", "--nocluster"]
