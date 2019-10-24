#!/bin/bash

#  NOTE: If you are running in Windows, you should comment out the below
#  aws variable, and uncomment the aws.cmd variable.

# Linux / Unix:
aws=aws

# Windows (Using Git Bash Shell):
#aws=aws.cmd

usage(){ echo "Usage: $0 -k key_pair_name -i key_pair_path";}

argCount=0
#Build command from arguments
while getopts "k:i:" opt; do
        case $opt in
                k)
                        key_pair_name=$OPTARG;((++argCount))
                        ;;
                i)      private_key_file=$OPTARG;((++argCount))
                        ;;
        esac
done

if [ $argCount -lt 2 ]
        then
                usage
                exit 1
fi

$aws ec2 create-key-pair --key-name=$key_pair_name --query 'KeyMaterial' --output text > "${private_key_file}"
