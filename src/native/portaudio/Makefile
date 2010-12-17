PORTAUDIO_HOME?=../../../../portaudio-20101214
SPEEX_HOME?=../../../../speex-1.2rc1

CC=gcc -O2
TARGET_BASENAME=jnportaudio

OS=$(shell $(CC) -dumpmachine | sed -e s/.*-apple-.*/mac/ -e s/.*-linux-.*/linux/ -e s/.*-.*-mingw32/windows/)
ifeq "$(OS)" "mac"
  JAVA_HOME?=/Developer/SDKs/MacOSX10.4u.sdk/System/Library/Frameworks/JavaVM.framework/Versions/1.5
  CC:=$(CC) -arch i386 -arch ppc -arch x86_64 -mmacosx-version-min=10.4
  CPPFLAGS=-I$(JAVA_HOME)/Headers 
  LDFLAGS=-dynamiclib
  LIBS=-framework AudioToolbox -framework AudioUnit -framework CoreAudio -framework Carbon \
       -lpthread -pthread -lm -dynamic -lportaudio -lspeexdsp
  TARGET=../../../lib/native/mac/lib$(TARGET_BASENAME).jnilib
else
  ARCH=$(shell $(CC) -dumpmachine | sed -e s/x86_64-.*/-64/ -e s/i.86-.*//)
  LDFLAGS=-shared
  ifeq "$(OS)" "linux"
    JAVA_HOME?=/usr/lib/jvm/java-6-sun
    CPPFLAGS=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
    LIBS=-Wl,-Bstatic -lportaudio -lspeexdsp -Wl,-Bdynamic -lasound -lm -lpthread
    TARGET=../../../lib/native/$(OS)$(ARCH)/lib$(TARGET_BASENAME).so
  else ifeq "$(OS)" "windows"
    DXSDK_HOME?=/c/Users/lyubomir/Downloads/DXSDK_Jun10
    ifeq "$ARCH" "-64"
      JAVA_HOME?=C:/PROGRA~1/jdk
      LIBS=-L$(DXSDK_HOME)/Lib/x64
    else
      JAVA_HOME?=C:/PROGRA~2/jdk
      LIBS=-L$(DXSDK_HOME)/Lib/x86
    endif
    CPPFLAGS=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32 -I$(DXSDK_HOME)/Include
    LDFLAGS:=-Wl,--kill-at $(LDFLAGS)
    LIBS:=$(LIBS) -static -lportaudio -lspeexdsp -lwinmm -ldsound -lm -lstdc++ -lole32 -luuid
    TARGET=../../../lib/native/$(OS)$(ARCH)/$(TARGET_BASENAME).dll
  endif
endif

CPPFLAGS:=-Wall -Wreturn-type -DJNI_IMPLEMENTATION \
         -I$(SPEEX_HOME)/include  -I$(PORTAUDIO_HOME)/include \
         $(CPPFLAGS)
LDFLAGS:=-fPIC $(LDFLAGS)
LIBS:=-L$(PORTAUDIO_HOME)/lib/.libs -L$(SPEEX_HOME)/libspeex/.libs $(LIBS)

$(TARGET): net_java_sip_communicator_impl_neomedia_portaudio_PortAudio.c AudioQualityImprovement.c
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
	-strip $(TARGET)
