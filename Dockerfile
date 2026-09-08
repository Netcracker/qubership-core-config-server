FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.4.2@sha256:f1167b5ab05fc01059d87d56363647acd137156fd3b5a35ce7cbceec94115ff4
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]
