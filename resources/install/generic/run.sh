mkdir -p $HOME/.sip-communicator/log

export PATH=$PATH:native
java -classpath "lib/jdic-all.jar:lib/jdic_stub.jar:lib/felix.jar:lib/kxml-min.jar:lib/servicebinder.jar:lib/bundle/org.apache.felix.servicebinder-0.8.0-SNAPSHOT.jar:sc-bundles/util.jar" -Djava.library.path=native -Dfelix.config.properties=file:./lib/felix.client.run.properties -Djava.util.logging.config.file=lib/logging.properties org.apache.felix.main.Main
