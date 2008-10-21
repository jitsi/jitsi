# Author: Damian Minkov
# javah -classpath ../../../../classes/ -o AEGetURLEventHandler.h net.java.sip.communicator.util.launchutils.AEGetURLEventHandler

CC=gcc -arch i386 -arch ppc -arch x86_64
TARGET=libAEGetURLEventHandlerAgent.jnilib
JNI_INCLUDE_PATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Headers/
CFLAGS=-I$(JNI_INCLUDE_PATH)
LIBS=-framework Foundation
OBJS=AEGetURLEventHandlerAgent.o

all:$(TARGET)

clean:
	rm -rf $(TARGET) $(OBJS) *~

install:$(TARGET)
	cp $(TARGET) ../../../../lib/native/mac/

libAEGetURLEventHandlerAgent.jnilib:$(OBJS)
	$(CC) -bundle -o $(TARGET) $(LIBS) $(CFLAGS) $<
