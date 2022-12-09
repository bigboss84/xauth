#!/usr/bin/env sh

olddir="$PWD"

work_dir=$1
cert_name="rds-combined-ca-bundle"
#cert_name="rds-ca-2019-root"
cert_url="https://s3.amazonaws.com/rds-downloads/$cert_name.pem"

if [ -z "$work_dir" ]; then
    echo "error: no supplied work directory"
    exit
fi

cd "$work_dir"

###

private_key="$cert_name.private.key"
certificate_pem="$cert_name.certificate.pem"
certificate_p12="$cert_name.p12"
certificate_pwd="changeit"

openssl req -newkey rsa:2048 -x509 -keyout "$private_key" -out "$certificate_pem" -days 3650 -nodes -subj "/C=IT/ST=Milan/L=Milan/O=X-Auth/OU=IT Department/CN=xauth.id"
echo "Created CSR file $certificate_pem"

openssl pkcs12 -export -in "$certificate_pem" -inkey "$private_key" -out "$certificate_p12" -name "$cert_name" -passout "pass:$certificate_pwd"
echo "Generated PKCS12 certificate $certificate_p12"

###

echo "Downloading $cert_name certificate from $cert_url"
wget -q $cert_url

# Splitting bundle file
csplit -sk "$cert_name.pem" "/-BEGIN CERTIFICATE-/" "{$(grep -c 'BEGIN CERTIFICATE' "$cert_name.pem" | awk '{print $1 - 2}')}"

# Processing each certificate
for cert in xx*; do
  if [ -s $cert ]
  then
    # extract a human-readable alias from the cert
    alias=$(openssl x509 -noout -text -in $cert | perl -ne 'next unless /Subject:/; s/.*CN=//; print')
    echo "importing $alias"
    # import the cert into the java keystore
    keytool -import \
            -keystore $certificate_p12 \
            -storepass changeit -noprompt \
            -alias "$alias" -file $cert
  else
    echo "$cert file is empty"
  fi
  rm "$cert"
done

cd "$olddir"
