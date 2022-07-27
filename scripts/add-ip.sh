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
"Script to add IP addresses to the coffeebreak AWS Security Group

Assumptions:
    - You have ansible installed on your local machine
    - You have aws credentials

Arguments:
    --anywhere
        Allow access from anywhere to the RabbitMQ servers
    --ip
        IP address to add to coffeebreak security group to allow access
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

ALLOW_ANYWHERE=0
IP_ADDRESS=""
AWS_PROFILE="default"


while [ $# -gt 0 ]
do
    key="$1"

    case $key in

        --anywhere)
        ALLOW_ANYWHERE=1
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

        --ip)
        if [ $# -lt 2 ]; then
            formatlog "ERROR" "Missing Value for --ip" >&2
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

if [[ -z $IP_ADDRESS ]] && [[ "$ALLOW_ANYWHERE" -ne 1 ]]; then
    formatlog "ERROR" "Missing --ip"
    exit 1
fi

if [ "$ALLOW_ANYWHERE" -eq 1 ]; then
    IP_RULE="0.0.0.0/0"
else
    IP_RULE="${IP_ADDRESS}/32"
fi

# Debugging Printing Variables
formatlog "DEBUG" "AWS_PROFILE=${AWS_PROFILE}"
formatlog "DEBUG" "IP_ADDRESS=${IP_RULE}"

AWS_GROUPID=$(aws ec2 describe-security-groups --filter "Name=tag:Name,Values=coffeebreak-PublicAccessSecurityGroup" --query "SecurityGroups[].GroupId" --output=text)

###
# Main Execution
###

formatlog "INFO" "Adding IP (${IP_ADDRESS}) to Coffeebreak Security Group (${AWS_GROUPID})"
aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges="[{CidrIp=${IP_RULE}}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=5671,ToPort=5671,IpRanges="[{CidrIp=${IP_RULE}}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=15672,ToPort=15672,IpRanges="[{CidrIp=${IP_RULE}}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=25672,ToPort=25672,IpRanges="[{CidrIp=${IP_RULE}}]"

aws ec2 --profile=${AWS_PROFILE} \
    authorize-security-group-ingress \
    --group-id ${AWS_GROUPID} \
    --ip-permissions IpProtocol=tcp,FromPort=4369,ToPort=4369,IpRanges="[{CidrIp=${IP_RULE}}]"
