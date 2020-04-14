CC = cc -O2
TARGET_BASENAME = jmacosxaddrbook

JAVA_HOME = /Developer/SDKs/MacOSX10.4u.sdk/System/Library/Frameworks/JavaVM.framework/Versions/1.5

CC := $(CC) -arch i386 -arch ppc -arch x86_64 -mmacosx-version-min=10.4
CPPFLAGS = -Wall -Wreturn-type -DJNI_IMPLEMENTATION -I$(JAVA_HOME)/Headers -I..
LDFLAGS = -dynamiclib
LIBS = -framework AddressBook -framework Foundation
TARGET = ../../../../lib/native/mac/lib$(TARGET_BASENAME).jnilib

$(TARGET): \
		../AddrBookContactQuery.c \
		net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactQuery.m \
		net_java_sip_communicator_plugin_addrbook_macosx_MacOSXAddrBookContactSourceService.m
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
	-strip $(TARGET)
