# Dockerfile to create an image containing Structure
# and the REST interface to it. We'll expose just the
# REST interface.

from openjdk:8-jre

MAINTAINER Structure Team "structure@raaf.tech"

RUN apt-get update
RUN apt-get install -y --no-install-recommends \
		nmap \
		smbclient \
		nodejs \
		runit
RUN rm -rf /var/lib/apt/lists/*

COPY target/structure-0.9.0-SNAPSHOT-fat.jar /opt/structure/structure.jar
COPY src/main/resources/bundles /opt/structure/bundles/raaf
COPY bin/winexe-x86_64 /usr/bin/winexe

EXPOSE 3000

ENV STRUCTURE_HOME=/opt/structure/
ENV STRUCTURE_BUNDLES=${STRUCTURE_HOME}/bundles/

WORKDIR /opt/structure/
ENTRYPOINT ["java", "-jar", "/opt/structure/structure.jar", "-u"]
