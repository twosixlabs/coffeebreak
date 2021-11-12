#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Script to standup the coffeebreak backend in AWS
# -----------------------------------------------------------------------------

set -e
CALL_NAME="$0"
THIS_DIR=$(cd $(dirname ${BASH_SOURCE[0]}) >/dev/null 2>&1 && pwd)

###
# Helper functions
###

. ${THIS_DIR}/common-functions.sh

###
# Arguments
###


HELP=\
"Script to standup the coffeebreak backend in AWS

Assumptions:
    - You have ansible installed on your local machine
    - You have aws credentials

Arguments:
    --ip
        IP address to add to coffeebreak
    --cluster-size
        The size of the RabbitMQ cluster (and number of coffeebreak servers)
    --aws-instance-type
        AWS instance type, defaults to t3.micro
    --aws-profile
        AWS profile to use to connect to AWS, defaults to default. 
    --ssh-key-name
        The name of the AWS ssh key to create/use for the EC2-Instance
    --ssh-key
        The private key to use to connect to AWS instances
    --verbose
        Set verbosity
    -h, --help
        Print this message.

Examples:
    ./standup-backend.sh \
        --aws-profile=coffeebreak \
        --aws-instance-type=t3a.micro \
        --ssh-key-name=coffeebreak-2 \
        --ssh-key-private=~/.ssh/coffeebreak_rsa.pem \
        --ssh-key-public=~/.ssh/coffeebreak_rsa.pub \
        --cluster-size=3
"

CLOUDFORMATION_FILE="${THIS_DIR}/cloudformation/coffeebreak.yml"

# Manager IP Address
IP_ADDRESS=$(curl --silent checkip.amazonaws.com)

# AWS Args
AWS_SSH_KEY_NAME="coffeebreak"
AWS_INSTANCE_TYPE="t3.micro"
AWS_AMI="ami-0b69ea66ff7391e80"
AWS_PROFILE="default"

# Cluster Details
RABBITMQ_CLUSTER_SIZE=2

# Auth Config
SSH_KEY_PRIVATE="~/.ssh/id_rsa"
SSH_KEY_PUBLIC="~/.ssh/id_rsa.pub"

while [ $# -gt 0 ]
do
    key="$1"

    case $key in

        --cluster-size)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --cluster-size" >&2
            exit 1
        fi
        RABBITMQ_CLUSTER_SIZE="$2"
        shift
        shift
        ;;

        --cluster-size=*)
        RABBITMQ_CLUSTER_SIZE="${1#*=}"
        shift
        ;;

        --aws-instance-type)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --aws-instance-type" >&2
            exit 1
        fi
        AWS_INSTANCE_TYPE="$2"
        shift
        shift
        ;;

        --aws-instance-type=*)
        AWS_INSTANCE_TYPE="${1#*=}"
        shift
        ;;

        --aws-profile)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --aws-profile" >&2
            exit 1
        fi
        AWS_PROFILE="$2"
        shift
        shift
        ;;

        --aws-profile=*)
        AWS_PROFILE="${1#*=}"
        shift
        ;;

        --ssh-key-name)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --ssh-key-name" >&2
            exit 1
        fi
        AWS_SSH_KEY_NAME="$2"
        shift
        shift
        ;;

        --ssh-key-name=*)
        AWS_SSH_KEY_NAME="${1#*=}"
        shift
        ;;

        --ssh-key-private)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --ssh-key-private" >&2
            exit 1
        fi
        SSH_KEY_PRIVATE="$2"
        shift
        shift
        ;;

        --ssh-key-private=*)
        SSH_KEY_PRIVATE="${1#*=}"
        shift
        ;;

        --ssh-key-public)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --ssh-key-public" >&2
            exit 1
        fi
        SSH_KEY_PUBLIC="$2"
        shift
        shift
        ;;

        --ssh-key-public=*)
        SSH_KEY_PUBLIC="${1#*=}"
        shift
        ;;

        -v|--verbose)
        set +x
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

# Debugging Printing Variables
formatlog "DEBUG" "CLOUDFORMATION_FILE=${CLOUDFORMATION_FILE}"
formatlog "DEBUG" "IP_ADDRESS=${IP_ADDRESS}"
formatlog "DEBUG" "AWS_SSH_KEY_NAME=${AWS_SSH_KEY_NAME}"
formatlog "DEBUG" "AWS_INSTANCE_TYPE=${AWS_INSTANCE_TYPE}"
formatlog "DEBUG" "AWS_AMI=${AWS_AMI}"
formatlog "DEBUG" "AWS_PROFILE=${AWS_PROFILE}"
formatlog "DEBUG" "RABBITMQ_CLUSTER_SIZE=${RABBITMQ_CLUSTER_SIZE}"
formatlog "DEBUG" "SSH_KEY_PUBLIC=${SSH_KEY_PUBLIC}"
formatlog "DEBUG" "SSH_KEY_PRIVATE=${SSH_KEY_PRIVATE}"

###
# Main Execution
###

formatlog "INFO" "Standing Up Coffeebreak"

AWS_PROFILE=${AWS_PROFILE} ansible-playbook -vv ${THIS_DIR}/ansible/playbooks/coffeebreak-standup.yml \
    --forks=${RABBITMQ_CLUSTER_SIZE} \
    --inventory=${THIS_DIR}/ansible/inventory/aws_ec2.yml \
    --private-key=${SSH_KEY_PRIVATE} \
    --user=ec2-user \
    --extra-vars="{\"numSecondaryNodes\": \"$(expr ${RABBITMQ_CLUSTER_SIZE} - 1)\", \"cloudformationFile\": \"${CLOUDFORMATION_FILE}\", \"sshKeyName\": \"${AWS_SSH_KEY_NAME}\", \"sshPublickKey\": \"${SSH_KEY_PUBLIC}\", \"amazonLinuxAMIID\": \"${AWS_AMI}\", \"amazonInstanceType\": \"${AWS_INSTANCE_TYPE}\", \"awsProfile\": \"${AWS_PROFILE}\", \"managerIpAddress\": \"${IP_ADDRESS}/32\"}"

formatlog "INFO" "AWS Instance Details:"
ansible-inventory -i ansible/inventory/aws_ec2.yml --graph
