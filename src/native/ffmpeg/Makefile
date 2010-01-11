FFMPEG_HOME=/home/seb/svn_work/ffmpeg
JAVA_HOME=/usr/lib/jvm/java-6-sun
X264_HOME=/home/seb/svn_work/x264-snapshot-20091208-2245

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -I$(JAVA_HOME)/include \
         -I$(FFMPEG_HOME) 

# Uncomment section for your OS AND comment out the others

# MacOS X configuration
#CPPFLAGS+=-I$(JAVA_HOME)/Headers/
#LDFLAGS=-dynamiclib -Wl,-read_only_relocs,suppress -arch ppc -arch x86_64 -arch i386
#TARGET=../../../lib/native/mac/libffmpeg.jnilib

# Windows configuration
# CPPFLAGS+=-I$(JAVA_HOME)/include/win32
#LDFLAGS=-Wl,--kill-at -shared
# Choose between 32-bit or 64-bit
#TARGET=../../../lib/native/windows/ffmpeg.dll
#TARGET=../../../lib/native/windows-64/ffmpeg.dll

# Linux configuration
CPPFLAGS+=-I$(JAVA_HOME)/include/linux
LDFLAGS=-fPIC -shared
# Choose between 32-bit or 64-bit
#TARGET=../../../lib/native/linux/libffmpeg.so
TARGET=../../../lib/native/linux-64/libffmpeg.so

LIBS=-L$(FFMPEG_HOME)/libavformat -L$(FFMPEG_HOME)/libavcodec -L$(FFMPEG_HOME)/libavutil \
     -L$(FFMPEG_HOME)/libswscale -L$(X264_HOME) \
     -lavformat -lavcodec -lavutil -lswscale -lx264

$(TARGET): net_java_sip_communicator_impl_neomedia_codec_video_FFMPEG.c net_java_sip_communicator_impl_neomedia_codec_video_FFMPEG.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
