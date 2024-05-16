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

- For usage of Docker in Jenkins do the following:

    ```bash
    # Add Jenkins user to the docker group
    sudo usermod -aG docker jenkins
    # Restart Jenkins to apply changes
    sudo systemctl restart jenkins
    ```

7. Enable docker service at AMI boot time:
    
    `sudo systemctl enable docker.service`

8. Start the Docker service:

    `sudo systemctl start docker.service`

### Verification

Now that both required software installed, we need to make sure it is working. Hence, type the following commands.

#### Finding status

Get the docker service status on your AMI instance, run:

`sudo systemctl status docker.service`

Outputs:

```bash
docker.service - Docker Application Container Engine
Loaded: loaded (/usr/lib/systemd/system/docker.service; enabled; vendor preset: disabled)
Active: active (running) since Wed 2021-09-08 05:03:52 EDT; 18s ago
Docs: https://docs.docker.com
Process: 3295 ExecStartPre=/usr/libexec/docker/docker-setup-runtimes.sh (code=exited, status=0/SUCCESS)
Process: 3289 ExecStartPre=/bin/mkdir -p /run/docker (code=exited, status=0/SUCCESS)
Main PID: 3312 (dockerd)
    Tasks: 9
   Memory: 39.9M
   CGroup: /system.slice/docker.service
           └─3312 /usr/bin/dockerd -H fd:// --containerd=/run/containerd/c...

Sep 08 05:03:51 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:51 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:51 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:51 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:52 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:52 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:52 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:52 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
Sep 08 05:03:52 amazon.example.local systemd[1]: Started Docker Applicatio...
Sep 08 05:03:52 amazon.example.local dockerd[3312]: time="2021-09-08T05:03...
```

Hint: Some lines were ellipsized, use -l to show in full.


#### Getting docker version info on Amazon Linux

The docker-compose is installed in ‘/usr/local/bin’ directory and it may not be on your PATH settings. To see current PATH settings run the echo command/printf command:

`echo "$PATH"`

To add `/usr/local/bin/` path to your bash startup file such as `~/.profile` or `~/.bash_profile` using the export command:

`export PATH=$PATH:/usr/local/bin`
