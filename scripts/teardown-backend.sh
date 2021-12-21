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

