CC = gcc -O2
OUTLOOK_MAPI_HEADERS ?= C:/Users/lyubomir/Downloads/Outlook2010MAPIHeaders
TARGET_BASENAME = jmsoutlookaddrbook

ARCH = $(shell $(CC) -dumpmachine | sed -e s/x86_64-.*/-64/ -e s/i.86-.*// -e s/mingw32//)
ifeq "$(ARCH)" "-64"
	JAVA_HOME ?= C:/PROGRA~1/jdk
else
	JAVA_HOME ?= C:/PROGRA~2/jdk
endif

CPPFLAGS = \
	-Wall -Wreturn-type \
	-DWINVER=0x0502 -D_WIN32_WINNT=0x0502 \
	-DJNI_IMPLEMENTATION \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32 -I$(OUTLOOK_MAPI_HEADERS) -I..
LDFLAGS = -shared -Wl,--kill-at -Wl,--subsystem,windows
LIBS = -ladvapi32 -luuid
TARGET = ../../../../lib/native/windows$(ARCH)/$(TARGET_BASENAME).dll

$(TARGET): \
		../AddrBookContactQuery.c \
		MsOutlookMAPIHResultException.cxx \
		net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.cxx \
		net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.cxx
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
	-strip $(TARGET)
