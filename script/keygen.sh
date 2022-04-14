#!/bin/bash

path="."
name="id_rsa"

while getopts "h?n:p:" opt; do
    case "$opt" in
    h|\?)
        echo "USAGE"
        echo "  ./keygen.sh -n <keyname> -p <path>"
        echo
        exit 0
        ;;
    n)  name=$OPTARG
        printf "Keyname: \e[1;37m$name\e[0m\n"
        ;;
    p)  path=$OPTARG
        printf "Path: \e[1;37m$path\e[0m\n"
        ;;
    esac
done

rsa_private_pem="$path/$name-rsa.private.pem"
rsa_private_der="$path/$name-rsa.private.der"
rsa_public_der="$path/$name-rsa.public.der"

openssl genrsa -out "$rsa_private_pem" 2048 2>/dev/null
printf "(PEM) Generated private key \e[1;37m$rsa_private_pem\e[0m\n"

openssl pkcs8 -topk8 -inform PEM -outform DER -in "$rsa_private_pem" -out "$rsa_private_der" -nocrypt 2>/dev/null
printf "(DER) Generated public key \e[1;37m$rsa_private_der\e[0m\n"

openssl rsa -in "$rsa_private_pem" -pubout -outform DER -out "$rsa_public_der" 2>/dev/null
printf "(DER) Generated private key \e[1;37m$rsa_public_der\e[0m\n"

echo done