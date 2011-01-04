CXX = c++ -O2
OUTLOOK_MAPI_HEADERS ?= /c/Users/lyubomir/Downloads/Outlook2010MAPIHeaders
TARGET_BASENAME = jmsoutlook.dll

ARCH = $(shell $(CXX) -dumpmachine | sed -e s/x86_64-.*/-64/ -e s/i.86-.*//)
ifeq "$(ARCH)" "-64"
	JAVA_HOME ?= C:/PROGRA~1/jdk
else
	JAVA_HOME ?= C:/PROGRA~2/jdk
endif

CPPFLAGS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32 -I$(OUTLOOK_MAPI_HEADERS)
LDFLAGS = -shared -Wl,--kill-at
LIBS = -lmapi32
TARGET = ../../../../lib/native/windows$(ARCH)/jmsoutlookaddrbook.dll

$(TARGET): \
		MsOutlookMAPIHResultException.cpp \
		net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactQuery.cpp \
		net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.c
	$(CXX) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
	-strip $(TARGET)
