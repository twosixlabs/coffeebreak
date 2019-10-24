#!/bin/bash

#  NOTE: If you are running in Windows, you should comment out the below
#  aws variable, and uncomment the aws.cmd variable.

# Linux / Unix:
aws=aws

# Windows (Using Git Bash Shell):
#aws=aws.cmd

#These are the default security groups, EC2 image, and EC2 type.

#  This is a default docker security group that allows traffic over
#  various default ports... We can dynamically add custom security groups later
security_group="cb-default"
key_pair_name=""
private_key=""
ec2_image="ami-0b69ea66ff7391e80"
ec2_type="t2.micro"
timer=720 # 12 hours

# Random values shared by the two instances:
export LC_CTYPE=C
# Erlang cookie:
erlang_cookie=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 24 | head -n 1)
echo ${erlang_cookie}
# AWS instance tag:
instance_tag=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 8 | head -n 1)
echo ${instance_tag}

#  This script is responsible for creating and running RabbitMQ on two
#  Amazon EC2 instances.
#  The parameters required are:
#  -  the RSA key pair name associated with your account,
#  -  the path to your RSA private key.
#  -  and a config file with your AWS CLI key ID and key.

#  Additionally, the file 'ebs-mapping.json' must be in the same directory.

#  This script will start two EC2 instances, affix TLS certificates,
#    and start a RabbitMQ cluster on those two instances.

usage(){ echo "Usage: $0 -k key_pair_name -i key_pair_path -c config_file";}

argCount=0
#Build command from arguments
while getopts "k:i:t:e:s:g:c:a:" opt; do
        case $opt in
                k)
                        key_pair_name=$OPTARG;((++argCount))
                        ;;
                i)      private_key=$OPTARG;((++argCount))
                        ;;
                t)      timer=$OPTARG;
                        ;;
                e)      ec2_image=$OPTARG;
                        ;;
                s)      ec2_type=$OPTARG;
                        ;;
                g)      security_group=$OPTARG;
                        ;;
		c)	config_file=$OPTARG;((++argCount))
			;;
		a)	autorun_script=$OPTARG;
			;;
        esac
done

if [ $argCount -lt 3 ]
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
#                     Start & Configure EC2 Instances                   #
#########################################################################


#  This JSON file should be in the same directory as this script
#  To adjust storage options (such as the size of the EBS volume),
#  edit this JSON file. Note that the default size is 50GB which should
#  be big enough for all the demos.
ebs_storage_mapping="file://ebs-mapping.json"

configureAWSCLI locally
echo ""
echo "[!] AWS CLI configured locally"

#Command to start the EC2 Instance
$aws ec2 run-instances --image-id $ec2_image --count 2 \
--instance-type $ec2_type --key-name $key_pair_name \
--security-group-ids $security_group \
--block-device-mappings $ebs_storage_mapping \
--tag-specifications "ResourceType=instance,Tags=[{Key=rabbit_cluster,Value=$instance_tag}]" \
--instance-initiated-shutdown-behavior terminate > /dev/null

echo "[*] Spinning up EC2 instances..."
sleep 5
id=($($aws ec2 describe-instances --filter \
     "Name=instance-state-name,Values=pending" \
     "Name=key-name,Values=${key_pair_name}" \
     | grep INSTANCES | awk '{print $8}'))

echo ${id[0]}, ${id[1]}

ip=($($aws ec2 describe-instances --filter \
     "Name=instance-id,Values=[${id[0]},${id[1]}]" \
     --query "Reservations[].Instances[].PublicIpAddress" \
     --output=text | tr -d '\r'))

echo ${ip[0]}, ${ip[1]}

ipv6=($($aws ec2 describe-instances --filter \
     "Name=instance-id,Values=[${id[0]},${id[1]}]" \
     --query "Reservations[].Instances[].NetworkInterfaces[].Ipv6Addresses[*]" \
     --output=text | tr -d '\r'))

echo ${ipv6[0]}, ${ipv6[1]}

publicdns=($($aws ec2 describe-instances --filter \
     "Name=instance-id,Values=[${id[0]},${id[1]}]" \
     --query "Reservations[].Instances[].PublicDnsName" \
     --output=text | tr -d '\r'))

echo ${publicdns[0]}, ${publicdns[1]}

privatedns=($($aws ec2 describe-instances --filter \
     "Name=instance-id,Values=[${id[0]},${id[1]}]" \
     --query "Reservations[].Instances[].PrivateDnsName" \
     --output=text | tr -d '\r'))

echo ${privatedns[0]}, ${privatedns[1]}

echo "[!] EC2 instances started at" ${ip[0]} ", " ${ip[1]}
echo "[!] EC2 IPV6 addresses assigned:" ${ipv6[0]} ", " ${ipv6[1]}


filter="Name=instance-state-name,Values=running,\
Name=instance-status.reachability,Values=passed"


echo "[*] waiting on instance to pass checks..."
echo "    - this will take about 5 minutes (go get coffee)"


eta=5
while true
do
        echo "[*] Running status checks... estimated time left: $eta minutes"
        temp="$($aws ec2 describe-instance-status \
              --instance-ids ${id[0]} \
              --filters $filter)"
        ((--eta))
        if [ "$temp" != "" ]
                then
                        break;
                fi
        sleep 60
done
while true
do
        echo "[*] Running status checks... estimated time left: $eta minutes"
        temp="$($aws ec2 describe-instance-status \
              --instance-ids ${id[1]} \
              --filters $filter)"
        if [ "$temp" != "" ]
                then
                        break;
                fi
        sleep 60
        ((--eta))
done
	

echo "[!] EC2 instance passed reachability checks"

##################################################################################

##################################################################################
#                    Copy and Run Install Script                                 #
##################################################################################

scp -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
-i $private_key rabbit-setup.sh ec2-user@${ip[0]}:/home/ec2-user/
scp -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
-i $private_key rabbit-setup.sh ec2-user@${ip[1]}:/home/ec2-user/

echo "[!] Installing on ${publicdns[0]}"

echo ssh -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
-i $private_key ec2-user@${ip[0]} "sudo bash ./rabbit-setup.sh ${erlang_cookie} ${publicdns[0]}"

ssh -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
-i $private_key ec2-user@${ip[0]} "sudo bash ./rabbit-setup.sh ${erlang_cookie} ${publicdns[0]}"

echo "[!] Installing on ${publicdns[1]}"

echo ssh -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
-i $private_key ec2-user@${ip[1]} "sudo bash ./rabbit-setup.sh ${erlang_cookie} ${publicdns[1]} ${privatedns[0]}"

ssh -q -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
-i $private_key ec2-user@${ip[1]} "sudo bash ./rabbit-setup.sh ${erlang_cookie} ${publicdns[1]} ${privatedns[0]}"

###

echo "[!] EC2 instances started at" ${ip[0]} ", " ${ip[1]}
echo "[!] EC2 IPV6 addresses assigned:" ${ipv6[0]} ", " ${ipv6[1]}
echo "[!] Both instances are tagged: rabbit_cluster=" ${instance_tag}
