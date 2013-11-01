mkdir -p $HOME/.sip-communicator/log

# Get architecture
ARCH=`uname -m | sed -e s/x86_64/64/ -e s/i.86/32/`

# Additionnal JVM arguments
CLIENTARGS=""

if [ $ARCH -eq 32 ]
then
    CLIENTARGS="-client -Xmx256m"
fi

export PATH=$PATH:native
java $CLIENTARGS -classpath "lib/felix.jar:sc-bundles/sc-launcher.jar:sc-bundles/util.jar:lib/" -Djava.library.path=native -Dfelix.config.properties=file:./lib/felix.client.run.properties -Djava.util.logging.config.file=lib/logging.properties net.java.sip.communicator.launcher.SIPCommunicator
