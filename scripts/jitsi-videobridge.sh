#!/bin/bash -ex

mkdir -p $HOME/jitsi
touch $HOME/jitsi/jvb.log
chmod 777 $HOME/jitsi
chmod 666 $HOME/jitsi/jvb.log


#export JAVA_HOME=$SNAP/usr/lib/jvm/java-11-openjdk-amd64
#export PATH=$JAVA_HOME/bin:$PATH

export JITSI_LOGFILE="$HOME/jitsi/jvb.log"
export JVB_HOSTNAME="jitsi"
export JVB_PORT="5347"
export JVB_SECRET="Mwqz0j5J"
export JVB_OPTS="--apis=rest,xmpp"
export JAVA_SYS_PROPS="-Dnet.java.sip.communicator.SC_HOME_DIR_LOCATION=/etc/jitsi -Dnet.java.sip.communicator.SC_HOME_DIR_NAME=videobridge -Dnet.java.sip.communicator.SC_LOG_DIR_LOCATION=jitsi -Djava.util.logging.config.file=/etc/jitsi/videobridge/logging.properties JITSI_LOGFILE: jitsi/jvb.log"

"$SNAP/usr/share/jitsi-videobridge/jvb.sh" --host="${JVB_HOST:-localhost}" --domain="${JVB_HOSTNAME}" --port="${JVB_PORT}" --secret="${JVB_SECRET}" "${JVB_OPTS}" | tee -a "${JITSI_LOGFILE}"
