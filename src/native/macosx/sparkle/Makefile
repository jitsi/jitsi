# Author: Romain KUNTZ

CC=gcc
TARGET=libsparkle_init.dylib
JNI_INCLUDE_PATH=/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Headers/
CFLAGS=-I$(JNI_INCLUDE_PATH)
LIBS=-framework AppKit,Foundation
OBJS=net_java_sip_communicator_impl_sparkle_SparkleActivator.o

all:$(TARGET)

clean:
	rm -rf $(TARGET) $(OBJS) *~

install:$(TARGET)
	cp $(TARGET) ../../../../lib/native/mac/

libsparkle_init.dylib:$(OBJS)
	$(CC) -dynamiclib -o $(TARGET) $(LIBS) $(CFLAGS) $<
