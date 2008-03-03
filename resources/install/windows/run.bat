mkdir "%UserProfile%/.sip-communicator/log"
set PATH=%PATH%;native
start javaw -classpath "lib/jdic-all.jar;lib/jdic_stub.jar;lib/felix.jar;lib/sc-launcher.jar;sc-bundles/util.jar" -Dfelix.config.properties=file:./lib/felix.client.run.properties -Djava.util.logging.config.file=lib/logging.properties net.java.sip.communicator.launcher.SIPCommunicator
servicebin