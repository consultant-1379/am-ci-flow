#Generate the private key of the root CA
openssl genrsa -out rootCA.key 2048

#Generate the self-signed root CA certificate
openssl req -x509 -new -nodes -key rootCA.key -subj "/CN=k8sCA" -days 100000 -out rootCA.crt -config ca.conf

#Review the certificate
openssl x509 -in rootCA.crt -text

#server
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -config ssl.conf
openssl x509 -req -in server.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out server.crt -days 30065 -extensions v3_ext -extfile ssl.conf
