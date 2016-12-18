#!/bin/bash
#
# Startup script for a spring boot project
#
# chkconfig: - 84 16
# description:

source /etc/service-profile

SERVICE_DIR=/opt/$SERVICE_NAME
PATH_TO_JAR=$SERVICE_DIR/$SERVICE_NAME.jar
PID_PATH_NAME=$SERVICE_DIR/$SERVICE_NAME.pid
LOG_FILE=$SERVICE_DIR/log/$SERVICE_NAME.log

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ -f $PID_PATH_NAME ] ; then
            if [ pgrep `cat $PID_PATH_NAME` > /dev/null 2>&1 ] ; then
                echo "$SERVICE_NAME is already running ..."
                exit 1
            fi
        fi

        JAVA_OPTS="$JVM_OPTS \
         --spring.profiles.active=$SERVICE_ENVIRONMENT \
         --logging.file=$LOG_FILE \
         --spring.pidfile=$PID_PATH_NAME \
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

        cd $SERVICE_DIR
        su -m $SERVICE_USER -c "java -jar $PATH_TO_JAR $JAVA_OPTS > $SERVICE_DIR/log/console.log 2>&1  &"
        echo "$SERVICE_NAME started ..."
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;

    restart)
        $0 stop
        $0 start
    ;;

    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
esac
