# Dockerfile to create an image containing Structure
# and the REST interface to it. We'll expose just the
# REST interface.

FROM openjdk:8-jre

MAINTAINER Structure Team "structure@raaf.tech"

RUN echo "deb http://ppa.launchpad.net/ansible/ansible/ubuntu trusty main" >> /etc/apt/sources.list.d/ansible.list
RUN echo "deb http://deb.nodesource.com/node_8.x trusty main" >> /etc/apt/sources.list.d/nodejs.list
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 93C4A3FD7BB9C367 1655A0AB68576280
RUN apt-get update
RUN apt-get install -y --no-install-recommends \
		nmap \
		smbclient \
		nodejs \
		ansible \
		runit
RUN rm -rf /var/lib/apt/lists/*

ENV STRUCTURE_HOME=/opt/structure/
ENV STRUCTURE_BUNDLES=${STRUCTURE_HOME}/bundles/

COPY target/structure-0.9.0-SNAPSHOT-fat.jar ${STRUCTURE_HOME}/structure.jar
COPY rest ${STRUCTURE_HOME}/rest
COPY tools ${STRUCTURE_HOME}/tools
COPY src/main/resources/bundles ${STRUCTURE_HOME}/bundles/raaf
COPY docker/bin/winexe-x86_64 /usr/bin/winexe
COPY docker/service/structure /etc/service/structure/run
COPY docker/service/rest /etc/service/rest/run

ADD https://raw.githubusercontent.com/fabric8io-images/java/master/images/jboss/openjdk8/jdk/container-limits /opt/container-limits
RUN chmod 755 /opt/container-limits

EXPOSE 3000

WORKDIR ${STRUCTURE_HOME}
ENTRYPOINT ["runsvdir", "-P", "/etc/service"]
