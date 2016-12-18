#!/bin/sh

#
# Usage: ./install.sh
#

source ./service-profile

yum -y remove java*
yum -y install java-1.8.0-openjdk

#------------------------------
# Download
#------------------------------
echo "DOWNLOADING..."
aws s3 cp s3://io.tchepannou.kribi/repository/$SERVICE_NAME/$SERVICE_VERSION/$SERVICE_NAME.jar .

#------------------------------
# Create user
#------------------------------
echo "CREATING USER: $SERVUCE_USER..."
id -u webapp &>/dev/null || useradd $SERVICE_USER

#------------------------------
# Install application
#------------------------------
echo "INSTALLING APPLICATION..."
if [ ! -d "/opt/$SERVICE_NAME" ]; then
  mkdir /opt/$SERVICE_NAME
fi
if [ ! -d "/opt/$SERVICE_NAME/log" ]; then
  mkdir /opt/$SERVICE_NAME/log
fi
if [ ! -d "/opt/$SERVICE_NAME/config" ]; then
  mkdir /opt/$SERVICE_NAME/config
fi

cp $SERVICE_NAME.jar /opt/$SERVICE_NAME/$SERVICE_NAME.jar
chown -R $SERVICE_USER:$SERVICE_USER /opt/$SERVICE_NAME

#------------------------------
# startup script
#------------------------------
echo "INSTALLING INITD SCRIPTS..."
cp service-profile /etc/
cp initd.sh /etc/init.d/$SERVICE_NAME
chmod +x /etc/init.d/$SERVICE_NAME

/sbin/chkconfig --add $SERVICE_NAME
/sbin/chkconfig $SERVICE_NAME on


#------------------------------
# restart
#------------------------------
echo "RESTARTING SERVICE..."
/etc/init.d/$SERVICE_NAME stop
/etc/init.d/$SERVICE_NAME start
