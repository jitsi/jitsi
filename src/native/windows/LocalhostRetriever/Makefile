##
# \file Makefile
# \brief Win32 (Visual C++) makefile.
# \author Sebastien Vincent

CC = cl
JNI_HEADERS = /I%JAVA_HOME%\include /I%JAVA_HOME%\include\win32
CFLAGS = $(JNI_HEADERS)

LocalhostRetriever.dll: net_java_sip_communicator_impl_netaddr_Win32LocalhostRetriever.c
	$(CC) $(CFLAGS) /LD net_java_sip_communicator_impl_netaddr_Win32LocalhostRetriever.c iphlpapi.lib /Fe$@

clean:
	del *.exp *.lib *.dll *.obj 

