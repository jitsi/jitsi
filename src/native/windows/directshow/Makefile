##
# \file Makefile
# \brief Windows (Visual C++) makefile for jdirectshow DLL.
# \author Sebastien Vincent

CC = cl /W4 /wd4996 /EHsc /O2
JNI_HEADERS = /I%JAVA_HOME%\include /I%JAVA_HOME%\include\win32
LIBS = /link /out:jdirectshow.dll
CFLAGS = $(JNI_HEADERS)

OBJS = net_java_sip_communicator_impl_neomedia_directshow_DSManager.cpp net_java_sip_communicator_impl_neomedia_directshow_DSCaptureDevice.cpp net_java_sip_communicator_impl_neomedia_directshow_DSFormat.cpp ds_manager.cpp ds_capture_device.cpp 

jdirectshow.dll: clean 
	$(CC) $(CFLAGS) /LD $(OBJS) $(LIBS) ole32.lib oleaut32.lib user32.lib

install64: jdirectshow.dll
	copy jdirectshow.dll ..\..\..\..\lib\native\windows-64\

install32: jdirectshow.dll
	copy jdirectshow.dll ..\..\..\..\lib\native\windows\

clean:
	del *.exp *.lib *.dll *.obj *.manifest 

