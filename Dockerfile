# Dockerfile to create an image containing Structure
# We assume `bower install` has been run in web/

from openjdk:8-jre

MAINTAINER Structure Team "structure@raaf.tech"

RUN apt-get update
RUN apt-get install -y --no-install-recommends nmap smbclient
RUN rm -rf /var/lib/apt/lists/*

COPY target/structure-0.9.0-SNAPSHOT-fat.jar /opt/structure/structure.jar
COPY web /opt/structure/web
COPY bundles /opt/structure/bundles
COPY bin/winexe-x86_64 /usr/bin/winexe

EXPOSE 8080

WORKDIR /opt/structure/
ENTRYPOINT ["java", "-jar", "/opt/structure/structure.jar"]
