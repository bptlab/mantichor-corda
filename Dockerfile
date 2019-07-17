FROM node:10 as builder

COPY ./adapterServer /usr/src/myapp/adapterServer
COPY ./cordapp_template /usr/src/myapp/cordapp_template
COPY ./constants.properties /usr/src/myapp/constants.properties

WORKDIR /usr/src/myapp/adapterServer

RUN apt-get update \
    && apt-get install -y curl java-common locales procps

RUN curl 'https://d3pxv6yz143wms.cloudfront.net/8.212.04.2/java-1.8.0-amazon-corretto-jdk_8.212.04-2_amd64.deb' -o java-8-amazon-corretto-jdk.deb -s \
    && dpkg --install java-8-amazon-corretto-jdk.deb \
    && rm java-8-amazon-corretto-jdk.deb

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && locale-gen

ENV LANG="en_US.UTF-8" \
    LANGUAGE="en_US:en" \
    LC_ALL="en_US.UTF-8"


EXPOSE 8080

CMD ["node","index.js"]
