#!/bin/sh

sudo yum -y update
sudo yum -y install docker
sudo service docker start

echo "[!] Docker installed on EC2 instance" $id

echo hostname `hostname`
HOSTNAME=`hostname`

#Build rabbitmq docker container and enable management interface
cat > Dockerfile <<EOF
FROM amazonlinux:latest

# Install RabbitMQ.
RUN \
yum -y update && \
yum -y install curl && \
curl -s https://packagecloud.io/install/repositories/rabbitmq/erlang/script.rpm.sh | bash && \
yum install -y erlang && \
curl -s https://packagecloud.io/install/repositories/rabbitmq/rabbitmq-server/script.rpm.sh | bash &&\
yum install -y shadow-utils && \
yum install -y rabbitmq-server && \
yum install -y openssl && \
rabbitmq-plugins enable rabbitmq_management

ADD autorun.sh /home/rabbitmq/autorun.sh
RUN chmod +x /home/rabbitmq/autorun.sh

EXPOSE 5672
EXPOSE 15672
EXPOSE 4369
EXPOSE 5671
EXPOSE 25672

RUN /home/rabbitmq/autorun.sh $*

EOF

#start docker container
docker build -t rabbitserv . && docker run -p 15672:15672 -p 5671:5671 -p 4369:4369 -p 25672:25672 -d -it --hostname $HOSTNAME rabbitserv

echo "[*] Starting rabbitmq..."
docker exec $(docker container ls -aq) rabbitmq-server start &
echo "[*] waiting for rabbitmq to start..."
sleep 10
echo "[!] DONE."
# Change default password

echo "[*] changing rabbitmq default password..."
docker exec $(docker container ls -aq) rabbitmqctl change_password guest caffein8 &
echo "[!] DONE."
# If second node, shut down, configure clustering, and restart


if [ $# -gt 1 ]
then
        echo "[!] joining cluster!"
        docker exec $(docker container ls -aq) rabbitmqctl stop_app
        docker exec $(docker container ls -aq) rabbitmqctl reset
        docker exec $(docker container ls -aq) rabbitmqctl join_cluster rabbit@$2
        docker exec $(docker container ls -aq) rabbitmqctl start_app
fi

echo "[!] Finished!"
echo ""
echo "PRESS CTRL+C TO CONTINUE..."
