mkdir -p $HOME/.sip-communicator/log
cd `dirname $0`

export PATH=$PATH:native
export JAVA_HOME=jre
${JAVA_HOME}/bin/java -classpath "lib/felix.jar:sc-bundles/sc-launcher.jar:sc-bundles/dnsjava.jar:sc-bundles/util.jar:lib/" -Djna.library.path=native -Djava.library.path=native -Dfelix.config.properties=file:./lib/felix.client.run.properties -Djava.util.logging.config.file=lib/logging.properties net.java.sip.communicator.launcher.SIPCommunicator
