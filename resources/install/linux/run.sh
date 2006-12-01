mkdir -p $HOME/.sip-communicator/log

export PATH=$PATH:native
export JAVA_HOME=jre
${JAVA_HOME}/bin/java -classpath "lib/BrowserLauncher2.jar:lib/felix.jar:lib/kxml-min.jar:lib/servicebinder.jar:lib/bundle/org.apache.felix.servicebinder-0.8.0-SNAPSHOT.jar:sc-bundles/util.jar" -Dicq.custom.message.charset=windows-1251 -Dfelix.config.properties=file:./lib/felix.client.run.properties -Djava.util.logging.config.file=lib/logging.properties org.apache.felix.main.Main
