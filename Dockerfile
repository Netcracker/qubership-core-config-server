FROM ghcr.io/netcracker/qubership-java-base:21-alpine-2.3.7@sha256:0703cab49931a129f16f39e11d108a4c15e9455a32508fc3f6690ae719aa826d
LABEL maintainer="qubership"

COPY --chown=10001:0 config-server-app/target/config-server-app-*.jar /app/config-server.jar

EXPOSE 8080

WORKDIR /app

CMD ["/usr/bin/java", "-Xmx512m", "-jar", "/app/config-server.jar"]
