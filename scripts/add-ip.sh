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
    ./add-ip.sh --ip=1.2.3.4
"

IP_ADDRESS=""
AWS_PROFILE="default"


while [ $# -gt 0 ]
do
    key="$1"

    case $key in

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

        --ip-size)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --cluster-size" >&2
            exit 1
        fi
        IP_ADDRESS="$2"
        shift
        shift
        ;;

        --ip=*)
        IP_ADDRESS="${1#*=}"
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

if [ -z $IP_ADDRESS ]; then
    formatlog "ERROR" "Missing --ip"
    exit 1
fi

# Debugging Printing Variables
formatlog "DEBUG" "AWS_PROFILE=${AWS_PROFILE}"
formatlog "DEBUG" "IP_ADDRESS=${IP_ADDRESS}"

AWS_GROUPID=$(aws ec2 describe-security-groups --filter "Name=tag:Name,Values=coffeebreak-PublicAccessSecurityGroup" --query "SecurityGroups[].GroupId" --output=text)

###
# Main Execution
###

formatlog "INFO" "Adding IP (${IP_ADDRESS}) to Coffeebreak Security Group (${AWS_GROUPID})"
aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges="[{CidrIp=${IP_ADDRESS}/32}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=5671,ToPort=5671,IpRanges="[{CidrIp=${IP_ADDRESS}/32}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=15672,ToPort=15672,IpRanges="[{CidrIp=${IP_ADDRESS}/32}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=25672,ToPort=25672,IpRanges="[{CidrIp=${IP_ADDRESS}/32}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=4369,ToPort=4369,IpRanges="[{CidrIp=${IP_ADDRESS}/32}]"
