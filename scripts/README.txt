Purpose:

These scripts will set up and tear down the message relay behind the
Coffeebreak app, running on public cloud infrastructure.

The message relay is a cluster of RabbitMQ servers running on AWS instances.

Steps:

1. Obtain an AWS account. (one-time)
2. Create security credentials: access keys and CLI key pairs (one-time)
3. Install the AWS Command Line Interface tools
4. Set up the AWS EC2 security group (one-time)
5. Start the AWS instances with the RabbitMQ server cluster.
6. Later, shut down the AWS instances.

In detail:

1. Obtain an AWS account. (one-time)

You will need an AWS account. If you already have one, you can use it. If you
do not, you can sign up for one at https://aws.amazon.com. If this is a
new account, their Free Tier will let you run the servers approximately
half the time for the first year without charge.

2. Create security credentials: access keys and SSH key pairs (one-time)

AWS has two kinds of security credentials we'll use: access keys and SSH
key pairs. The access keys are used when we start, configure and shut down
the AWS instances. The SSH key pairs are used when we run commands remotely
to install, configure and run RabbitMQ on each instance once it is running.

2a. Access keys

To generate your access key ID and secret access key, refer to
https://docs.aws.amazon.com/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys

Place the access key ID and the secret access key into a file in this
directory, on one line, separated by a comma. See 'secrets-file' for
an example.

2b. SSH keys

To generate the SSH keys and associate them with the account, refer to
https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html
to either have Amazon generate one for you or to upload one you've
generated locally.

Make these files only readable by you, and place them in a directory
that's readable only by you.

3. Install the AWS Command Line Interface tools and log in

Follow the instructions in:

https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html

Next, type 'aws configure' and answer the questions:
- AWS access key ID
- AWS secret access key
- Region (us-east-1 is a good default)
- Format (text)

4. Set up the AWS EC2 security group (one-time)

Run aws-create-security-group.sh in this directory.

5. Start the AWS instances with the RabbitMQ server cluster.

Run aws-coffee.sh in this directory with the arguments:
-i <path to private SSH key file>
-k <name of keypair for this user>
-c <secrets file name>

The last lines of output of the script will show the IP addresses of the
two servers.  Enter each of these into the Cofeebreak app on one of the phones.

The last lines of outout of the script will also include a "tag identifier".
Save that for when it's time to shut down the servers.

6. Later, shut down the AWS instances.

To shut down the servers, run aws-coffee-teardown.sh with the arguments:
-c <secrets file name>
-z <tag>

