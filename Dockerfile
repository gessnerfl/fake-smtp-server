FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.12_7

ARG APP_VERSION

VOLUME /tmp

EXPOSE 5080
EXPOSE 5081
EXPOSE 5025

ADD build/libs/fake-smtp-server-$APP_VERSION.jar /opt/fake-smtp-server.jar
RUN ["touch", "/opt/fake-smtp-server.jar"]
ENV JAVA_OPTS=""
ENTRYPOINT exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /opt/fake-smtp-server.jar