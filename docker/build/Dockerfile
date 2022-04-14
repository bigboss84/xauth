FROM alpine:3.7

ARG ENVIRONMENT
ENV ENVIRONMENT=$ENVIRONMENT

ARG APP_VERSION
ENV APP_VERSION=$APP_VERSION

ARG APP_NAME
ENV APP_NAME=$APP_NAME \
    SCALA_VERSION="2.12.6" \
    SCALA_HOME=/usr/share/scala

ARG APP_ARCHIVE="$APP_NAME-$APP_VERSION.zip"

RUN apk update; \
    apk upgrade; \
    apk add openjdk8; \
    apk add nss; \
    apk add --virtual=.build-dependencies wget ca-certificates; \
    apk add bash; \
    cd "/tmp"; \
    wget "http://downloads.lightbend.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz"; \
    tar xzf "scala-${SCALA_VERSION}.tgz"; \
    mkdir "${SCALA_HOME}"; \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat; \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}"; \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/"; \
    apk del .build-dependencies; \
    rm -rf "/tmp/"* \
    rm -fr /var/cache/apk/*

# Creating user
RUN addgroup -S xauth; \
    adduser -Ss /bin/bash -h /home/xauth -G xauth xauth

WORKDIR /home/xauth

# Copyng application files
RUN mkdir -p .jwt/keys/
ADD ../../script/keygen.sh /home/.jwt/keys/
ADD $APP_ARCHIVE .
RUN unzip $APP_ARCHIVE
RUN rm $APP_ARCHIVE

RUN chown xauth:xauth *

ENV APP_DIRECTORY="/home/xauth/$APP_NAME-$APP_VERSION"

CMD $APP_DIRECTORY/bin/$APP_NAME \
    -Dconfig.file=$APP_DIRECTORY/conf/application-$ENVIRONMENT.conf \
    -Dplay.http.secret.key=mysecret
