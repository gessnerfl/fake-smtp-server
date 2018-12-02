FROM adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine

VOLUME /tmp

EXPOSE 5080
EXPOSE 5081
EXPOSE 5025

ADD build/libs/fake-smtp-server.jar /opt/fake-smtp-server.jar
RUN sh -c 'touch /opt/fake-smtp-server.jar'
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/fake-smtp-server.jar" ]