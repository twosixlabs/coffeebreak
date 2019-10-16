# Start with a bare Amazon Linux EC2 instance
# Run as root
# Arguments:
# - $1: Erlang cookie
# - $2: External DNS name
# - $3: (for second server) Amazon internal DNS name of first server

# Update packages

yum update

# Install erlang

curl -s https://packagecloud.io/install/repositories/rabbitmq/erlang/script.rpm.sh | sudo bash

# Install RabbitMQ

curl -s https://packagecloud.io/install/repositories/rabbitmq/rabbitmq-server/script.rpm.sh | sudo bash

# Turn on management plugin

rabbitmq-plugins enable rabbitmq_management

# Generate certificates and keys -- need these to be present and valid for TLS

## CA directory

mkdir /home/testca
cd /home/testca
mkdir certs private
chmod 700 private
echo 01 > serial
touch index.txt

cat > openssl.cnf <<EOF
[ ca ]
default_ca = testca

[ testca ]
dir = .
certificate = $dir/ca_certificate.pem
database = $dir/index.txt
new_certs_dir = $dir/certs
private_key = $dir/private/ca_private_key.pem
serial = $dir/serial

default_crl_days = 7
default_days = 365
default_md = sha256

policy = testca_policy
x509_extensions = certificate_extensions

[ testca_policy ]
commonName = supplied
stateOrProvinceName = optional
countryName = optional
emailAddress = optional
organizationName = optional
organizationalUnitName = optional
domainComponent = optional

[ certificate_extensions ]
basicConstraints = CA:false

[ req ]
default_bits = 2048
default_keyfile = ./private/ca_private_key.pem
default_md = sha256
prompt = yes
distinguished_name = root_ca_distinguished_name
x509_extensions = root_ca_extensions

[ root_ca_distinguished_name ]
commonName = hostname

[ root_ca_extensions ]
basicConstraints = CA:true
keyUsage = keyCertSign, cRLSign

[ client_ca_extensions ]
basicConstraints = CA:false
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = 1.3.6.1.5.5.7.3.2

[ server_ca_extensions ]
basicConstraints = CA:false
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = 1.3.6.1.5.5.7.3.1
EOF

## Generate CA key/cert

openssl req -x509 -config openssl.cnf -newkey rsa:2048 -days 365 \
    -out ca_certificate.pem -outform PEM -subj /CN=MyTestCA/ -nodes
openssl x509 -in ca_certificate.pem -out ca_certificate.cer -outform DER

## Generate and sign server cert

cd ..
mkdir server
cd server
openssl genrsa -out private_key.pem 2048
openssl req -new -key private_key.pem -out req.pem -outform PEM \
    -subj /CN=$2/O=server/ -nodes
cd ../testca
openssl ca -config openssl.cnf -in ../server/req.pem -out \
    ../server/server_certificate.pem -notext -batch -extensions server_ca_extensions


# Create RabbitMQ config file

cat > /etc/rabbitmq/rabbitmq.config <<EOF
[
        { rabbit, [
                { loopback_users, [ ] },
                { tcp_listeners, [5672] },
                { ssl_listeners, [5671] },
                { ssl_options, [
                    {cacertfile,"/home/testca/cacert.pem"},
                    {certfile,"/home/server/cert.pem"},
                    {keyfile,"/home/server/key.pem"},
                    {verify,verify_none},
                    {fail_if_no_peer_cert,false},
                    {versions, ['tlsv1.2', 'tlsv1.1']}
                ]}
        ] }
].
EOF

# Set erlang cookie

echo $1 > /var/lib/rabbitmq/.erlang.cookie

# If second node, configure clustering

rabbitmqctl join_cluster rabbit@$3

# Start

service rabbitmq-server start
