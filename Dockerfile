FROM java:openjdk-8-jre-alpine

MAINTAINER Brian Schlining <bschlining@gmail.com>

ENV APP_HOME /opt/annosaurus

RUN mkdir -p ${APP_HOME}

COPY target/pack/ ${APP_HOME}/

EXPOSE 8080

ENTRYPOINT $APP_HOME/bin/jetty-main