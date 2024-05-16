# Install docker on AWS Linux image

### Installing Docker on Amazon Linux 2

The procedure to install Docker on AMI 2 (Amazon Linux 2) running on either EC2 or Lightsail instance is as follows:

1. Login into remote AWS server using the ssh command:

    `ssh ec2-user@ec2-ip-address-dns-name-here`

2. Apply pending updates using the yum command:

    `sudo yum update`

3. Search for Docker package:

    `sudo yum search docker`

4. Get version information:

    `sudo yum info docker`

5. Install docker, run:

    `sudo yum install docker`

6. Add group membership for the default ec2-user so you can run all docker commands without using the sudo command:

    ```bash
    sudo usermod -a -G docker ec2-user
    id ec2-user
    # Reload a Linux user's group assignments to docker w/o logout
    newgrp docker
    ```