#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Script to teardown the coffeebreak backend in AWS
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
"Script to teardown the coffeebreak backend in AWS

Assumptions:
    - You have ansible installed on your local machine
    - You have aws credentials

Arguments:
    --verbose
        Set verbosity
    -h, --help
        Print this message.

Examples:
    ./teardown-backend.sh --verbose
"

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
formatlog "DEBUG" "AWS_PROFILE=${AWS_PROFILE}"

###
# Main Execution
###

formatlog "INFO" "Tearing Down"

AWS_PROFILE=${AWS_PROFILE} ansible-playbook ${THIS_DIR}/ansible/playbooks/coffeebreak-teardown.yml --forks=1

