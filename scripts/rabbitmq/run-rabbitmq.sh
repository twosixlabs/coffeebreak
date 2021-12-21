#!/usr/bin/env bash

# Copyright 2021 Raytheon BBN Technologies Corp.
# Copyright 2021 Two Six Technologies
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# -----------------------------------------------------------------------------
# Script to run RabbitMQ and configure it as a cluster for coffeebreak
# -----------------------------------------------------------------------------

set -e
CALL_NAME="$0"
THIS_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) >/dev/null 2>&1 && pwd)

###
# Helper functions
###

formatlog() {
    LOG_LEVELS=("DEBUG" "INFO" "WARNING" "ERROR")
    if [ "$1" = "ERROR" ]; then
        RED='\033[0;31m'
        NO_COLOR='\033[0m'
        printf "${RED}%s (%s): %s${NO_COLOR}\n" "$(date +%c)" "${1}" "${2}"
    elif [ "$1" = "WARNING" ]; then
        YELLOW='\033[0;33m'
        NO_COLOR='\033[0m'
        printf "${YELLOW}%s (%s): %s${NO_COLOR}\n" "$(date +%c)" "${1}" "${2}"
    elif [ ! "$(echo "${LOG_LEVELS[@]}" | grep -co "${1}")" = "1" ]; then
        echo "$(date +%c): ${1}"
    else
        echo "$(date +%c) (${1}): ${2}"
    fi
}


###
# Arguments
###


HELP=\
"Script to run RabbitMQ and configure it as a cluster for coffeebreak

Arguments:
    --rabbitmq-primary
        The hostname of the leader rabbitmq node to join the cluster of
    --verbose
        Set verbosity
    -h, --help
        Print this message.

Examples:
    ./run-rabbitmq
"

RABBITMQ_PRIMARY=""

while [ $# -gt 0 ]
do
    key="$1"

    case $key in

        --rabbitmq-primary)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --rabbitmq-primary" >&2
            exit 1
        fi
        RABBITMQ_PRIMARY="$2"
        shift
        shift
        ;;
        --rabbitmq-primary=*)
        RABBITMQ_PRIMARY="${1#*=}"
        shift
        ;;

        -v|--verbose)
        VERBOSE="true"
        shift
        ;;

        -h|--help)
        printf "%s" "${HELP}"
        shift
        exit 1;
        ;;

        "")
        # empty string. do nothing.
        shift
        ;;

        *)
        formatlog "ERROR" "${CALL_NAME} unknown argument \"$1\""
        exit 1
        ;;
    esac
done

###
# Main Execution
###

formatlog "INFO" "Running RabbitMQ"

# TODO, get from arg
HOSTNAME=$(hostname)
formatlog "INFO" "Setting hostname to $HOSTNAME"

#start docker container
formatlog "INFO" "Building Docker Image"
docker build -t rabbitmq-server /home/ec2-user/rabbitmq/

if [ "$(docker container ls -aq -fname=rabbitmq*)" ]; then
    formatlog "INFO" "Removing Old rabbitmq-server"
    docker rm --force rabbitmq-server
fi

formatlog "INFO" "Running Docker Container"
docker run -d -it\
    -p 4369:4369 \
    -p 5671:5671 \
    -p 15672:15672 \
    -p 25672:25672 \
    --name=rabbitmq-server \
    --hostname $HOSTNAME \
    rabbitmq-server &

formatlog "INFO" "Wating 10 Seconds for RabbitMQ Container to start"
sleep 10

formatlog "INFO" "Starting RabbitMQ Server"
docker exec rabbitmq-server rabbitmq-server start &

formatlog "INFO" "Sleeping for 10 seconds"
sleep 10

# If second node, shut down, configure clustering, and restart
if [ ! -z $RABBITMQ_PRIMARY ]; then
    formatlog "INFO" "Secondary Node (${HOSTNAME}) Found; Joining RabbitMQ Cluster on $RABBITMQ_PRIMARY"
    formatlog "INFO" "Stopping the App RabbitMQ Server"
    docker exec rabbitmq-server rabbitmqctl stop_app
    formatlog "INFO" "Reseting RabbitMQ Server"
    docker exec rabbitmq-server rabbitmqctl reset
    formatlog "INFO" "Join Cluster"
    docker exec rabbitmq-server rabbitmqctl join_cluster rabbit@$RABBITMQ_PRIMARY
    formatlog "INFO" "Start App"
    docker exec rabbitmq-server rabbitmqctl start_app
fi

formatlog "INFO" "run-rabbitmq complete"
