FROM openjdk:8u151-jdk-alpine
LABEL maintainer="Prabhneet S Arora <prabhneet.java@gmail.com>"
ENV REFRESHED_AT 2017-12-17
EXPOSE 8080
RUN set -ex \
  && apk update \
  && apk upgrade \
  && apk add git
RUN mkdir /voter \
  && git clone --depth 1 --branch build-artifacts-gke \
      "https://github.com/prabhneetgit/voter-service.git" /voter \
  && cd /voter \
  && mv voter-*.jar voter-service.jar
CMD [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "voter/voter-service.jar" ]
