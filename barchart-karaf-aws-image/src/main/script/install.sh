#!/bin/bash

# script location
WORK=$(dirname $0 )

echo "##################################################"

echo "project : ${project.artifactId}"

echo "##################################################"

# verify location
pwd

echo "##################################################"

# verify upload
ls -las $WORK

echo "##################################################"

# disable apt-get and oracle dialogs
export DEBIAN_FRONTEND=noninteractive
echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
	
# install applications
add-apt-repository --yes ppa:webupd8team/java
apt-get --yes update
apt-get --yes install oracle-jdk7-installer
apt-get --yes install mc tar wget zip unzip secure-delete
apt-get --yes upgrade
apt-get --yes dist-upgrade

echo "##################################################"
						
# verify java
java -version 2>&1

echo "##################################################"

ADMIN_GROUP="ubuntu"
ADMIN_USER="ubuntu"

KARAF_HOME="/var/karaf"
KARAF_GROUP="karaf"
KARAF_USER="karaf"

KARAF_ARCHIVE="${karafFinalName}"

# setup karaf user
addgroup --system $KARAF_GROUP
adduser --system --ingroup $KARAF_GROUP --home $KARAF_HOME $KARAF_USER
adduser $ADMIN_USER $KARAF_GROUP

# restrict karaf home access
chown --changes --recursive $ADMIN_GROUP:$KARAF_USER $KARAF_HOME
chmod --changes --recursive o-rwx,g+rw,ugo-s $KARAF_HOME
find $KARAF_HOME -type d -exec chmod --changes g+s {} \;

echo "##################################################"

# copy karaf home resources
cp --recursive $WORK $KARAF_HOME

# unpack karaf application
unzip $KARAF_ARCHIVE -d $KARAF_HOME

# cleanup work folder
srm -r -l -v $WORK

echo "##################################################"

sleep 3s

exit 0
