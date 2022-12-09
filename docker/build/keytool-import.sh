#!/bin/sh

APPLICATION_DIR=$1
CERT_NAME="rds-combined-ca-bundle"

cd "$APPLICATION_DIR/conf"

CERT_URL="https://s3.amazonaws.com/rds-downloads/$CERT_NAME.pem"

echo "Downloading $CERT_NAME certificate from $CERT_URL"
wget -q $CERT_URL

echo "Downloaded $APPLICATION_DIR/$CERT_NAME.pem"
echo "Importing keystore..."

keytool -import -v -trustcacerts -alias "$CERT_NAME" -file "$CERT_NAME.pem" -keystore "$CERT_NAME.p12" -storepass changeit -noprompt
echo "$CERT_NAME imported."
