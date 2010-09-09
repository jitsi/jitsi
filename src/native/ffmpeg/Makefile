FFMPEG_HOME?=/Users/Lubomir/src/ffmpeg-r23181
#JAVA_HOME?=/usr/lib/jvm/java-6-sun
JAVA_HOME=/System/Library/Frameworks/JavaVM.framework
#JAVA_HOME=C:\Progra~1\Java\jdk1.6.0_18
MP3LAME_HOME?=/Users/Lubomir/src/lame-3.98.4
X264_HOME?=/Users/Lubomir/src/x264-snapshot-20100518-2245

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -I$(JAVA_HOME)/include \
         -I$(FFMPEG_HOME) 

# Uncomment section for your OS AND comment out the others

# Linux configuration
#CPPFLAGS+=-I$(JAVA_HOME)/include/linux -Wl,-Bsymbolic
#LDFLAGS=-fPIC -shared
# Choose between 32-bit or 64-bit
#TARGET=../../../lib/native/linux/libffmpeg.so
#TARGET=../../../lib/native/linux-64/libffmpeg.so

# MacOS X configuration
CPPFLAGS+=-I$(JAVA_HOME)/Headers/
LDFLAGS=-fPIC -dynamiclib -Wl,-read_only_relocs,suppress
TARGET=../../../lib/native/mac/libffmpeg.jnilib

# Windows configuration
#CPPFLAGS+=-I$(JAVA_HOME)/include/win32
#LDFLAGS=-Wl,--kill-at -shared
# Choose between 32-bit or 64-bit
#TARGET=../../../lib/native/windows/ffmpeg.dll
#TARGET=../../../lib/native/windows-64/ffmpeg.dll

# FreeBSD configuration
#CPPFLAGS+=-I$(JAVA_HOME)/include/freebsd
#LDFLAGS=-fPIC -shared
# Choose between 32-bit or 64-bit
#TARGET=../../../lib/native/freebsd/libffmpeg.so
#TARGET=../../../lib/native/freebsd-64/libffmpeg.so

LIBS=-L$(FFMPEG_HOME)/libavcodec -L$(FFMPEG_HOME)/libavformat -L$(FFMPEG_HOME)/libavutil \
     -L$(FFMPEG_HOME)/libswscale \
     -L$(MP3LAME_HOME)/libmp3lame/.libs \
     -L$(X264_HOME) \
     -lavformat -lavcodec -lavutil \
     -lswscale \
     -lmp3lame \
     -lx264

$(TARGET): net_java_sip_communicator_impl_neomedia_codec_FFmpeg.c net_java_sip_communicator_impl_neomedia_codec_FFmpeg.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
