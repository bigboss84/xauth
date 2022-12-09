#!/bin/sh

APPLICATION_DIR=$1
#CERT_NAME="rds-combined-ca-bundle"
CERT_NAME="rds-ca-2019-root"

cd "$APPLICATION_DIR"

CERT_URL="https://s3.amazonaws.com/rds-downloads/$CERT_NAME.pem"

echo "Downloading $CERT_NAME certificate from $CERT_URL"
wget -q $CERT_URL

echo "Downloaded $APPLICATION_DIR/$CERT_NAME.pem"

private_key="$CERT_NAME.private.key"
certificate_pem="$CERT_NAME.certificate.pem"
certificate_p12="$CERT_NAME.p12"
certificate_jks="$CERT_NAME.jks"
certificate_pwd="changeit"

openssl req -newkey rsa:2048 -x509 -keyout "$private_key" -out "$certificate_pem" -days 3650 -nodes -subj "/C=IT/ST=Milan/L=Milan/O=X-Auth/OU=IT Department/CN=xauth.id"
echo "Created CSR file $certificate_pem"

openssl pkcs12 -export -in "$certificate_pem" -inkey "$private_key" -out "$certificate_p12" -name "$CERT_NAME" -passout "pass:$certificate_pwd"
echo "Generated PKCS12 certificate $certificate_p12"

#keytool -importkeystore -srckeystore "$certificate_p12" -srcstoretype pkcs12 -destkeystore "$certificate_jks"

#rm "$private_key" "$certificate_pem" "$CERT_NAME.pem"