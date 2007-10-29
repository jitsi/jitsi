mkdir "%UserProfile%/.sip-communicator/log"
set PATH=%PATH%;native
start javaw -classpath "lib/jdic-all.jar;lib/jdic_stub.jar;lib/felix.jar;sc-bundles/util.jar" -Dfelix.config.properties=file:./lib/felix.client.run.properties -Djava.util.logging.config.file=lib/logging.properties org.apache.felix.main.Main
servicebin