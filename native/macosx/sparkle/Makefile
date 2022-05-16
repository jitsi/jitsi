# Author: Romain KUNTZ
# Requires the Sparkle.framework installed in /Library/Frameworks
# The Framework is available at http://sparkle.andymatuschak.org/
# export MACOSX_DEPLOYMENT_TARGET=10.5

CC=gcc -arch x86_64 -arch i386 -arch ppc -mmacosx-version-min=10.5
TARGET=libsparkle_init.dylib
JNI_INCLUDE_PATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Headers/
CFLAGS=-I$(JNI_INCLUDE_PATH) -I/System/Library/Frameworks/Sparkle.framework/Versions/A/Headers/
LIBS=-framework AppKit -framework Foundation -framework Sparkle
OBJS=net_java_sip_communicator_impl_sparkle_SparkleActivator.o

all:$(TARGET)

clean:
	rm -rf $(TARGET) $(OBJS) *~

install:$(TARGET)
	cp $(TARGET) ../../../../lib/native/mac/

libsparkle_init.dylib:$(OBJS)
	$(CC) -dynamiclib -o $(TARGET) $(LIBS) $(CFLAGS) $<
	install_name_tool -change "@loader_path/../Frameworks/Sparkle.framework/Versions/A/Sparkle" "@executable_path/../Frameworks/Sparkle.framework/Versions/A/Sparkle" libsparkle_init.dylib

