FROM alpine:latest

ARG ENVIRONMENT
ENV ENVIRONMENT=$ENVIRONMENT

ARG APP_SECRET
ENV APP_SECRET=$APP_SECRET

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

# Installing required packages to import rds-combined-ca-bundle.pem into keystore
RUN apk add coreutils openssl perl

# Importing certificates from S3
#ADD import-rds-certs.sh /
#RUN chmod +x import-rds-certs.sh; \
#    JAVA_HOME=/usr/lib/jvm/default-jvm; \
#    ./import-rds-certs.sh; \
#    rm import-rds-certs.sh

# Cleaning
#RUN apk del curl coreutils openssl perl

# Creating user
RUN addgroup -S xauth; \
    adduser -Ss /bin/bash -h /home/xauth -G xauth xauth

WORKDIR /home/xauth

# Copyng application files
RUN mkdir -p .jwt/keys/
ADD keygen.sh .jwt/keys/
ADD $APP_ARCHIVE .
RUN unzip $APP_ARCHIVE
RUN rm $APP_ARCHIVE

RUN chown xauth:xauth *

ENV APP_DIRECTORY="/home/xauth/$APP_NAME-$APP_VERSION"

# Importing certificates
WORKDIR $APP_DIRECTORY
#ADD rds-combined-ca-bundle.p12 $APP_DIRECTORY/conf
RUN mkdir script
ADD import-rds-certs.sh script
RUN chmod +x script/import-rds-certs.sh
RUN ./script/import-rds-certs.sh $APP_DIRECTORY/conf

# Launching service
CMD $APP_DIRECTORY/bin/$APP_NAME \
    -Dconfig.file=$APP_DIRECTORY/conf/application-$ENVIRONMENT.conf \
    -Dplay.http.secret.key=$APP_SECRET
