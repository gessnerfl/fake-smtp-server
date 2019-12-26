FROM amazoncorretto:11.0.5

VOLUME /tmp

EXPOSE 5080
EXPOSE 5081
EXPOSE 5025

ADD build/libs/fake-smtp-server.jar /opt/fake-smtp-server.jar
RUN ["touch", "/opt/fake-smtp-server.jar"]
ENV JAVA_OPTS=""
ENTRYPOINT exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /opt/fake-smtp-server.jar