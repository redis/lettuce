#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CA_DIR=work/ca
TRUSTSTORE_FILE=work/truststore.jks
KEYSTORE_FILE=work/keystore.jks

if [[ -d work/ca ]] ; then
    rm -Rf ${CA_DIR}
fi

if [[ -f ${TRUSTSTORE_FILE} ]] ; then
    rm -Rf ${TRUSTSTORE_FILE}
fi

if [[ -f ${KEYSTORE_FILE} ]] ; then
    rm -Rf ${KEYSTORE_FILE}
fi

if [ ! -x "$(which openssl)" ] ; then
   echo "[ERROR] No openssl in PATH"
   exit 1
fi

KEYTOOL=keytool

if [  ! -x "${KEYTOOL}" ] ; then
   KEYTOOL=${JAVA_HOME}/bin/keytool
fi

if [  ! -x "${KEYTOOL}" ] ; then
   echo "[ERROR] No keytool in PATH/JAVA_HOME"
   exit 1
fi

mkdir -p ${CA_DIR}/private ${CA_DIR}/certs ${CA_DIR}/crl ${CA_DIR}/csr ${CA_DIR}/newcerts ${CA_DIR}/intermediate

echo "[INFO] Generating CA private key"
# Less bits = less secure = faster to generate
openssl genrsa -passout pass:changeit -aes256 -out ${CA_DIR}/private/ca.key.pem 2048

chmod 400 ${CA_DIR}/private/ca.key.pem

echo "[INFO] Generating CA certificate"
openssl req -config ${DIR}/openssl.cnf \
      -key ${CA_DIR}/private/ca.key.pem \
      -new -x509 -days 7300 -sha256 -extensions v3_ca \
      -out ${CA_DIR}/certs/ca.cert.pem \
      -passin pass:changeit \
      -subj "/C=NN/ST=Unknown/L=Unknown/O=lettuce/CN=CA Certificate"

echo "[INFO] Prepare CA database"
echo 1000 > ${CA_DIR}/serial
touch ${CA_DIR}/index.txt

function generateKey {

    host=$1

    echo "[INFO] Generating server private key"
    openssl genrsa -aes256 \
          -passout pass:changeit \
          -out ${CA_DIR}/private/${host}.key.pem 2048

    openssl rsa -in ${CA_DIR}/private/${host}.key.pem \
          -out ${CA_DIR}/private/${host}.decrypted.key.pem \
          -passin pass:changeit

    chmod 400 ${CA_DIR}/private/${host}.key.pem
    chmod 400 ${CA_DIR}/private/${host}.decrypted.key.pem

    echo "[INFO] Generating server certificate request"
    openssl req -config ${DIR}/openssl.cnf \
          -key ${CA_DIR}/private/${host}.key.pem \
          -passin pass:changeit \
          -new -sha256 -out ${CA_DIR}/csr/${host}.csr.pem \
          -subj "/C=NN/ST=Unknown/L=Unknown/O=lettuce/CN=${host}"

    echo "[INFO] Signing certificate request"
    openssl ca -config ${DIR}/openssl.cnf \
          -extensions server_cert -days 375 -notext -md sha256 \
          -passin pass:changeit \
          -batch \
          -in ${CA_DIR}/csr/${host}.csr.pem \
          -out ${CA_DIR}/certs/${host}.cert.pem
}

generateKey "localhost"
generateKey "foo-host"

echo "[INFO] Generating client auth private key"
openssl genrsa -aes256 \
      -passout pass:changeit \
      -out ${CA_DIR}/private/client.key.pem 2048

openssl rsa -in ${CA_DIR}/private/client.key.pem \
      -out ${CA_DIR}/private/client.decrypted.key.pem \
      -passin pass:changeit

chmod 400 ${CA_DIR}/private/client.key.pem

echo "[INFO] Generating client certificate request"
openssl req -config ${DIR}/openssl.cnf \
      -key ${CA_DIR}/private/client.key.pem \
      -passin pass:changeit \
      -new -sha256 -out ${CA_DIR}/csr/client.csr.pem \
      -subj "/C=NN/ST=Unknown/L=Unknown/O=lettuce/CN=client"

echo "[INFO] Signing certificate request"
openssl ca -config ${DIR}/openssl.cnf \
      -extensions usr_cert -days 375 -notext -md sha256 \
      -passin pass:changeit \
      -batch \
      -in ${CA_DIR}/csr/client.csr.pem \
      -out ${CA_DIR}/certs/client.cert.pem

echo "[INFO] Creating PKCS12 file with client certificate"
openssl pkcs12 -export -clcerts \
      -in ${CA_DIR}/certs/client.cert.pem \
      -inkey ${CA_DIR}/private/client.decrypted.key.pem \
      -passout pass:changeit \
      -out ${CA_DIR}/client.p12

${KEYTOOL} -importcert -keystore ${TRUSTSTORE_FILE} -file ${CA_DIR}/certs/ca.cert.pem -noprompt -storepass changeit
${KEYTOOL} -importkeystore \
                              -srckeystore ${CA_DIR}/client.p12 -srcstoretype PKCS12 -srcstorepass changeit\
                              -destkeystore ${KEYSTORE_FILE} -deststoretype JKS \
                              -noprompt -storepass changeit
