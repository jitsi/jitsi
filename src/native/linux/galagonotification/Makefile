JAVA_HOME?=/usr/lib/jvm/java-6-sun

ARCH=$(shell uname -m | sed -e s/x86_64/-64/ -e s/i.86//)
TARGET=../../../../lib/native/linux$(ARCH)/libgalagonotification.so

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -fPIC \
         -Wall -Wreturn-type \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
         $(shell pkg-config --cflags dbus-1)
LDFLAGS=-shared
LIBS=$(shell pkg-config --libs dbus-1)

$(TARGET): net_java_sip_communicator_impl_galagonotification_GalagoNotification.c net_java_sip_communicator_impl_galagonotification_GalagoNotification.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
