FROM maven:3.5-jdk-8 as BUILD

RUN mkdir /tmp/src;
WORKDIR /tmp/src/
ADD . .
RUN mvn clean install


FROM openjdk:8-jdk
MAINTAINER Matthieu Rémy

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64
RUN apt-get update \
  && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
RUN mkdir /opt/karaf;
COPY --from=BUILD /tmp/src/target/karaf-sandbox-1.0.0.tar.gz /opt/karaf-sandbox-1.0.0.tar.gz
RUN tar --strip-components=1 -C /opt/karaf -xzf /opt/karaf-sandbox-1.0.0.tar.gz; \
    rm /opt/karaf-sandbox-1.0.0.tar.gz
VOLUME ["/opt/karaf/deploy"]
EXPOSE 1099 8101 44444 8181
COPY entrypoint.sh /usr/local/bin/docker-entrypoint
RUN chmod +x /usr/local/bin/docker-entrypoint
ENTRYPOINT ["docker-entrypoint"]