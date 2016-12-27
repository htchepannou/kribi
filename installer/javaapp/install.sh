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
# RUN
#------------------------------
echo "RUNNING APP..."

LOG_FILE=/opt/$SERVICE_NAME/log/$SERVICE_NAME.log

JAVA_OPTS="$JVM_OPTS \
 --spring.profiles.active=$SERVICE_ENVIRONMENT \
 --logging.file=$LOG_FILE \
 --application.version=$SERVICE_VERSION \
 -XX:+PrintGCDateStamps \
 -verbose:gc \
 -XX:+PrintGCDetails \
 -Xloggc:$SERVICE_DIR/gc.log \
 -XX:+UseGCLogFileRotation \
 -XX:NumberOfGCLogFiles=10 \
 -XX:GCLogFileSize=100M \
 -XX:+HeapDumpOnOutOfMemoryError \
 -XX:HeapDumpPath=$SERVICE_DIR/log/heap-`date +%s`.hprof"

cd /opt/$SERVICE_NAME
su -m $SERVICE_USER -c "java -jar $SERVICE_NAME.jar $JAVA_OPTS > log/console.log 2>&1  &"
