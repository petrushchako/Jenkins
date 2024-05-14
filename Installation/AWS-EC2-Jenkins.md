# Jenkins installation on EC2 (AWS)

### Prerequisites
Before we get the ball rolling, make sure you’ve got these:
- An Amazon Linux 2023 instance.
- Ensure your security groups are configured to allow traffic through port 8080. This is the default port Jenkins uses.


### Step 1: SSH Into Your Instance
- SSH into the Amazon Linux instance where we’re setting up Jenkins. Open up your terminal and run:

    ```bash
    ssh -i <your_key>.pem ec2-user@<your_amazon_linux_instance_ip>
    ```


### Step 2: Update the System
- It’s always a good practice to ensure that your system is up to date. Open a terminal window and enter the following command:

    ```bash
    sudo dnf update
    ```

### Step 3: Install Java
Jenkins is a Java application, so Java is a prerequisite. 

- Install Java with the following command:

    ```bash
    sudo dnf install java-17-amazon-corretto -y
    ```

- Verify the installation with:

    ```bash
    java -version
    ```


### Step 4: Add the Jenkins Repository
- Add the Jenkins repository to your system with the following commands:

    ```bash
    sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
    ```

- Import a key file from Jenkins-CI to enable installation from the package:
    
    ```bash
    sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
    ```

### Step 5: Install Jenkins
- Install Jenkins by running:
  
    ```bash
    sudo dnf install jenkins -y
    ```

### Step 6: Start and Enable Jenkins
- Enable and start the Jenkins service using:

    ```bash
    sudo systemctl enable jenkins
    sudo systemctl start jenkins
    ```

### Step 7: Access Jenkins
- Open your web browser and access Jenkins by navigating to:
    
    ```text
    http://your_amazon_linux_instance_ip:8080
    ```
You will see a setup wizard and be prompted to enter the administrator password.
