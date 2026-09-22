FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.5.5@sha256:960fca655cd1af315dba03b2c16b778cf4498af8fa081132f2afa03f396761cf
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]
