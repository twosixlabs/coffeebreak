#!/bin/bash

#  NOTE: If you are running in Windows, you should comment out the below
#  aws variable, and uncomment the aws.cmd variable.

# Linux / Unix:
aws=aws

# Windows (Using Git Bash Shell):
#aws=aws.cmd

#  This script is responsible for terminating two Amazon EC2 instances.
#  The parameters required are:
#  -  a config file with your AWS CLI key ID and key and
#  -  the instance tag we gave to the instances.

usage(){ echo "Usage: $0 -c config_file -z instance_tag";}

argCount=0
#Build command from arguments
while getopts "c:z:" opt; do
        case $opt in
		c)	config_file=$OPTARG;((++argCount))
			;;
		z)	instance_tag=$OPTARG;((++argCount))
			;;
        esac
done

if [ $argCount -lt 2 ]
        then
                usage
                exit 1
fi

access_id="$(cat $config_file | awk -F ',' '{print $1}')"
secret="$(cat $config_file | awk -F ',' '{print $2}')"

#  This function will be used both locally as well as on the EC2
#  instance. Please set your AWS ID and Secret Key for authentication.
configureAWSCLI()
{

if [ "$1" == "locally" ]; then
	printf "%s\n%s\nus-east-1\ntext\n" "$id" "$secret" | $aws configure
elif [ "$1" == "remote" ]; then
	printf "%s\n%s\nus-east-1\ntext\n" "$2" "$3" | aws configure
fi
}

#########################################################################
#                     Find and terminate EC2 instances	
#########################################################################


configureAWSCLI locally
echo ""
echo "[!] AWS CLI configured locally"

id=($($aws ec2 describe-instances --filter \
     "Name=tag:rabbit_cluster,Values=${instance_tag}" \
     | grep INSTANCES | awk '{print $8}'))

echo ${id[0]}, ${id[1]}

$aws ec2 terminate-instances --instance-ids ${id[0]} ${id[1]}
