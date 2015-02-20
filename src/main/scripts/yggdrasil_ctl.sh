#!/bin/bash
# -----------------------
# Purpose: Yggdrasil start|stop|restart|status.
# Description: init-like script, to be called from real init script.
# ------------------------

# ------------------------
# set yggdrasil environment:
${assembly.home.env.name}=$(cd $(dirname $0); pwd -P)
${assembly.config.env.name}="${assembly.home.env.name.ref}/config"
CP=${assembly.home.env.name.ref}/:${assembly.home.env.name.ref}/config/:${assembly.home.env.name.ref}/lib/*

source ${assembly.home.env.name.ref}/daemon-settings.sh

# ------------------------
# check yggdrasil environment:
[ -z "${assembly.runningmode.env.name.ref}" ] &&  echo "error: ${assembly.runningmode.env.scriptname} not set" && exit 1
[ ! -d "${assembly.home.env.name.ref}" ] && echo "error: ${assembly.home.env.name} not set" && exit 1
[ ! -d "${assembly.config.env.name.ref}" ] && echo "error: ${assembly.config.env.name} not set" && exit 1

# ------------------
# daemon settings:
YGGJAVACMD="$JAVA_HOME/bin/java ${JAVA_OPTS} -D${assembly.home.env.name}=${assembly.home.env.name.ref} -D${assembly.config.env.name}=${assembly.config.env.name.ref} -D${assembly.runningmode.env.name}=${assembly.runningmode.env.name.ref} -cp $CP"
DAEMON="$YGGJAVACMD ${assembly.main.class.name}"
YGGQUESTOPCMD="$YGGJAVACMD ${assembly.shutdown.class.name}"

PIDFILE=/home/$DUSER/$DNAM.pid
DOUT=/home/$DUSER/$DNAM.nohup.out

# debug:
#echo "DAEMON=$DAEMON"
#echo "YGGQUESTOPCMD=$YGGQUESTOPCMD"

# -------------------
# functions:

# Source function library:
if [ -f /etc/rc.d/init.d/functions ] 
then
    source /etc/rc.d/init.d/functions
else
    if [ -f /lib/lsb/init-functions ]
    then
        source /lib/lsb/init-functions
        source ${assembly.home.env.name.ref}/echo_functions.sh
    else
        echo "The file containing global shell-script helper functions was not found"
        exit 1  
    fi
fi

function start() {
    if [ -s $PIDFILE ]; 
    then
        echo "Error: $PIDFILE exists"
        echo "Perhaps $DNAM is already running, or it was stopped abnormally?"
        echo_failure; echo
        RETVAL=1
    else
        # start daemon:
        echo "Starting $DNAM ..."
        nohup $DAEMON > $DOUT 2>&1 &
        PID=$!
        # check if PID is running:
        sleep 1
        if ps -p $PID > /dev/null
        then
          echo $PID > $PIDFILE
          echo "Started PID=$PID"; echo_success; echo
          RETVAL=0
        else
          echo "Error: check $DOUT"
          echo_failure; echo
          RETVAL=1
        fi
    fi
    return $RETVAL
}

function stop() {
    echo "Stopping $DNAM ..."
    if [ ! -s $PIDFILE ]; 
    then
        echo "Error: $PIDFILE not found"
        echo_failure; echo
        RETVAL=1
    else
      # queue stop message:
      echo "Sending queue stop message..."
      $YGGQUESTOPCMD
      SLEEP_SECS=10
      echo "will now sleep for $SLEEP_SECS secs ..."
      sleep $SLEEP_SECS
      # kill daemon:
      PID=$(cat $PIDFILE)
      if ps -p $PID > /dev/null
      then
        echo "Killing PID=$PID ..."
        killproc -p $PIDFILE
        RETVAL=$?
        echo
        [ $RETVAL = 0 ] && rm -f "$PIDFILE"
      else
        echo "PID=$PID not running (closed via queue?)"
        rm -f "$PIDFILE"
        RETVAL=0
      fi
    fi
    return $RETVAL
}

function status() {
    if [ -s $PIDFILE ]
    then
      echo "$DNAM is running with pid: $(cat $PIDFILE)"
    else
      echo "$DNAM is not running"
    fi
    return 0
}

# ------------------
# Main:

# check user:
if [ $(/usr/bin/id -un) != "$DUSER" ]; 
then
 echo "Error: $0 must be run as $DUSER user"
 exit 1
fi

# apparently we need to run from ${assembly.home.env.name.ref}(!?):
cd ${assembly.home.env.name.ref} 

# actions:
case "$1" in
    'start')
        start
        ;;
    'stop')
        stop
        ;;
    'restart')
        stop
        sleep 2
        start
        ;;
    'status')
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        RETVAL=1
        ;;
esac

exit $RETVAL

