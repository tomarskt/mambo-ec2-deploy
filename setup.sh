#!/usr/bin/env bash

HADOOP_PACKAGE=hadoop-2.4.1
HBASE_PACKAGE=hbase-0.98.7-hadoop2
HADOOP_NFS_CONN=hadoop-nfsv3-connector
MVN_PACKAGE=apache-maven-3.0.5


# Setup the Hadoop packages
cp -rf packages/${HADOOP_PACKAGE} hadoop
cp -rf packages/${HBASE_PACKAGE} hbase
cp -rf package/${MVN_PACKAGE} /usr/local
cp -rf package/${HADOOP_NFS_CONN} hadoop-nfsv3-connector
# Copy the configuration files
mkdir /opt/mambo/mambo-ec2-deploy/hadoop/conf
cp configuration/3node/hadoop/conf/* /opt/mambo/mambo-ec2-deploy/hadoop/conf

# Setup keys
cp configuration/3node/keys/id_rsa_mambo /home/ec2-user/.ssh/
chmod 400 /home/ec2-user/.ssh/id_rsa_mambo
chown ec2-user:ec2-user /home/ec2-user/.ssh/id_rsa_mambo
cat configuration/3node/keys/id_rsa_mambo.pub >> /home/ec2-user/.ssh/authorized_keys
cat configuration/3node/keys/ssh_conf >> /home/ec2-user/.ssh/config
chown ec2-user:ec2-user /home/ec2-user/.ssh/config
chmod 400 /home/ec2-user/.ssh/config

# set directories and change permissions
mkdir /mnt/namenode
mkdir /mnt/datanode
mkdir /mnt/hadoop-temp

chown ec2-user /mnt/namenode
chown ec2-user /mnt/datanode
chown ec2-user /mnt/hadoop-temp
chown -R ec2-user /opt/mambo/mambo-ec2-deploy/hadoop/

#env
echo "export JAVA_HOME=/usr/lib/jvm/java-1.7.0/" >> /etc/profile
echo "export PATH=/usr/local/${MVN_PACKAGE}/bin:$PATH" >> /etc/profile

