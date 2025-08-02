#!/bin/bash

# Create CA private key
openssl genrsa -out ca-key.pem 4096

# Create CA certificate
openssl req -new -x509 -days 365 -key ca-key.pem -out ca.pem \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Signite/OU=IT Department/CN=Signite CA"

# Create MongoDB server private key
openssl genrsa -out mongodb-key.pem 4096

# Create MongoDB certificate signing request
openssl req -new -key mongodb-key.pem -out mongodb.csr \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Signite/OU=IT Department/CN=category-db-service"

# Create MongoDB certificate
openssl x509 -req -in mongodb.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial \
    -out mongodb-cert.pem -days 365

# Create client private key for Category Service
openssl genrsa -out client-key.pem 4096

# Create client certificate signing request
openssl req -new -key client-key.pem -out client.csr \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Signite/OU=IT Department/CN=category-service"

# Create client certificate
openssl x509 -req -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial \
    -out client-cert.pem -days 365

# Combine MongoDB cert and key
cat mongodb-cert.pem mongodb-key.pem > mongodb-combined.pem

echo "TLS certificates generated successfully!"
echo "Files created:"
echo "  - ca.pem (CA certificate)"
echo "  - mongodb-combined.pem (MongoDB server cert + key)"
echo "  - client-cert.pem (Client certificate)"
echo "  - client-key.pem (Client private key)"