FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.3.6@sha256:6925353c16218651bc911f9f64ae941eeabe99e7e54c24a9d63edb09c58342b9
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]
