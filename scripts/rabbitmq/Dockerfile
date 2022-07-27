FROM amazonlinux:latest
LABEL maintainer="Two Six Technologies <TBD@twosixlabs.com>"

###
# Setup environment
###

ENV APP_ROOT=/home/rabbitmq
ENV CA_ROOT=/home/testca
ENV SERVER_ROOT=/home/server

###
# Install Packages
###

# Install RabbitMQ and Linux Packages
RUN curl -s https://packagecloud.io/install/repositories/rabbitmq/erlang/script.rpm.sh | bash && \
    curl -s https://packagecloud.io/install/repositories/rabbitmq/rabbitmq-server/script.rpm.sh | bash && \
    yum -y update && \
    yum install -y curl erlang shadow-utils rabbitmq-server openssl && \
    rabbitmq-plugins enable rabbitmq_management

# Setup CA Directory
RUN mkdir -p ${CA_ROOT}/certs ${CA_ROOT}/private && \
    chmod 700 ${CA_ROOT}/private && \
    echo 01 > ${CA_ROOT}/serial && \
    touch ${CA_ROOT}/index.txt
ADD openssl.cnf ${CA_ROOT}/openssl.cnf

## Generate CA key/cert
RUN cd ${CA_ROOT} && \
    openssl req -x509 -config openssl.cnf -newkey rsa:2048 -days 365 -out cacert.pem -outform PEM -subj /CN=MyTestCA/ -nodes && \
    openssl x509 -in cacert.pem -out cacert.cer -outform DER

## Generate and sign server cert
RUN mkdir -p ${SERVER_ROOT} && \
    cd ${SERVER_ROOT} && \
    openssl genrsa -out key.pem 2048 && \
    openssl req -new -key key.pem -out req.pem -outform PEM -subj /O=server/ -nodes && \
    cd ${CA_ROOT} && \
    openssl ca -config openssl.cnf -in ../server/req.pem -out ../server/cert.pem -notext -batch -extensions server_ca_extensions && \
    chown rabbitmq ${SERVER_ROOT}/key.pem ${SERVER_ROOT}/cert.pem ${CA_ROOT}/cacert.pem && \
    chmod 600 ${SERVER_ROOT}/key.pem ${SERVER_ROOT}/cert.pem ${CA_ROOT}/cacert.pem

# Setup Rabbit Configs
RUN mkdir -p /etc/rabbitmq && \
    chown rabbitmq /etc/rabbitmq
ADD rabbitmq.config /etc/rabbitmq/rabbitmq.config
RUN chown rabbitmq /etc/rabbitmq/rabbitmq.config

# Set erlang cookie
RUN mkdir -p /var/lib/rabbitmq && \
    chown rabbitmq /var/lib/rabbitmq
ADD erlang-cookie /var/lib/rabbitmq/.erlang.cookie
RUN chown rabbitmq /var/lib/rabbitmq/.erlang.cookie &&\
    chmod 400 /var/lib/rabbitmq/.erlang.cookie

###
# Configure Image
###

# Ports to Expose for Docker
EXPOSE 5672
EXPOSE 15672
EXPOSE 4369
EXPOSE 5671
EXPOSE 25672

###
# Set Entrypoints
###

# Set last working dir back to app root
WORKDIR /home/rabbitmq
