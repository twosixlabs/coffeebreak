# Coffeebreak Backend Scripts

The following scripts allow users to quickly stand up a Coffeebreak RabbitMQ backend using AWS and Ansible. 

## Table of Contents

* [Dependencies](#dependencies)
* [Prerequisites](#prerequisites)
* [How To](#how-to)

## Dependencies

Dependencies for the scripts to run on a local machine include:

* ansible
* aws-cli

## Prerequisites

### AWS Account and Configuration

You will need an AWS account and have it properly configured on your local machine

 1. **Obtain an AWS account**

	If you already have one, you can use it. If you do not, you can sign up for one at https://aws.amazon.com. If this is a new account, their Free Tier will let you run the 1 server for approximately the first year without charge.

 2. **Create AWS access keys**

	AWS has two kinds of security credentials we'll use: access keys and SSH key pairs. The access keys are used when we start, configure and shut down the AWS instances. The SSH key pairs are used when we run commands remotely to install, configure and run RabbitMQ on each instance once it is running.

	To generate your access key ID and secret access key, refer to https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys

	Place the access key ID and the secret access key into a file in this directory, on one line, separated by a comma. See `secrets-file` for an example.

 3. **Install the AWS Command Line Interface**

	Follow the instructions in:

	https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html

 4. **Configure the AWS Command Line Interface**

	Run the `aws configure` command and answer the questions:

	* AWS access key ID
	* AWS secret access key
	* Region (us-east-1)
	* Format (text)

### Ansible

You will need an AWS account and have it properly configured on your local machine

1. **Install Ansible**

	Follow the instructions in:

	https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html

## How To

### How to Stand up a Coffeebreak Backend

```shell
localhost$ bash standup-backend.sh
```

Note: run `--help` for optional parameters if you would like to adjust the deployment

### How to Automatically Tear down a Coffeebreak Backend

```shell
localhost$ bash teardown-backend.sh
```

### How to Manually Tear down a Coffeebreak Backend

The Coffeebreak backend is created in AWS utilizing CloudFormation. To remove all items from AWS, you can manually delete the CloudFormation stack `coffeebreak` manually following the guide below:

https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-console-delete-stack.html

### How to Add an IP

The Coffeebreak backend is configured to only allow access to the IP address of the computer running the scripts. If you want to enable other IPs to be able to join the network, you need to run:

```shell
localhost$ bash add-ip.sh --ip=IP_TO_ADD
```

If you would like to open the network up to all incoming traffic, then you can run:

```shell
localhost$ bash add-ip.sh --ip=0.0.0.0
```