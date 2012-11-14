#!/bin/bash

ADMIN_GROUP="root"
ADMIN_USER="ubuntu"

KARAF_HOME="/var/karaf"
KARAF_GROUP="karaf"
KARAF_USER="karaf"

KARAF_ARCHIVE="${karafFinalName}"
KARAF_APP_DIR="app"

# identity during setup
export BARCHART_CONFIG_IDENTITY="default.aws.barchart.com"

# script location
WORK=$(dirname $0 )

echo "##################################################"
echo "### project : ${project.artifactId}"
echo "###"

echo "##################################################"
echo "### verify work location"
echo "###"

pwd

echo "##################################################"
echo "### verify upload contents"
echo "###"

ls -las $WORK

echo "##################################################"
echo "### configure kernel"
echo "###"

cp --verbose --force $WORK/sysctl-karaf.conf /etc/sysctl.d/
sysctl -p

echo "##################################################"
echo "### disable installer dialogs"
echo "###"

export DEBIAN_FRONTEND=noninteractive
echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
	
echo "##################################################"
echo "### install utilities"
echo "###"
add-apt-repository --yes ppa:webupd8team/java
apt-get --yes update
apt-get --yes install oracle-jdk7-installer
apt-get --yes install mc tar wget zip unzip secure-delete
apt-get --yes upgrade
apt-get --yes dist-upgrade

echo "##################################################"
echo "### verify java"
echo "###"
																																																
java -version 2>&1

echo "##################################################"
echo "### setup karaf user home"
echo "###"

addgroup --system $KARAF_GROUP
adduser  --system --shell=/bin/bash --ingroup $KARAF_GROUP --home $KARAF_HOME $KARAF_USER

echo "##################################################"
echo "### copy karaf home resources"
echo "###"

cp --verbose --force --recursive $WORK/. $KARAF_HOME

echo "##################################################"
echo "### unpack karaf application"
echo "###"

unzip -o $KARAF_HOME/$KARAF_ARCHIVE -d $KARAF_HOME

echo "##################################################"
echo "### remove application folder time stamp suffix"
echo "###"

mv --verbose "$KARAF_HOME/$KARAF_APP_DIR"* "$KARAF_HOME/$KARAF_APP_DIR" 

echo "##################################################"
echo "### setup karaf home access permissions"
echo "###"

chown --changes --recursive $KARAF_USER:$KARAF_GROUP $KARAF_HOME
chmod --changes --recursive o-rwx,g+rw,ugo-s $KARAF_HOME
find $KARAF_HOME -type d -exec chmod --changes g+s {} \;

echo "##################################################"
echo "### identity during setup : $BARCHART_CONFIG_IDENTITY"
echo "###"

echo "##################################################"
echo "### install tanuki service"
echo "###"

$KARAF_HOME/$KARAF_APP_DIR/nix-install.sh

echo "##################################################"
echo "### tanuki start"
echo "###"

$KARAF_HOME/$KARAF_APP_DIR/nix-start.sh

echo "##################################################"
echo "### await fetch config repo and maven artifacts"
echo "###"

sleep 30s

echo "##################################################"
echo "### tanuki stop"
echo "###"

$KARAF_HOME/$KARAF_APP_DIR/nix-stop.sh

echo "##################################################"
echo "### cleanup work folder"
echo "###"

srm -r -l -v $WORK

sleep 3s

exit 0
