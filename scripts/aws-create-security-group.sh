aws ec2 create-security-group --description test --group-name cb-default
aws ec2 authorize-security-group-ingress --group-name cb-default --port 5671 --protocol tcp --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-name cb-default --port 22 --protocol tcp --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-name cb-default --port 0-65535 --protocol tcp --source-group cb-default

