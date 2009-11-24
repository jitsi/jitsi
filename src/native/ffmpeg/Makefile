FFMPEG_HOME=/Users/Lubomir/src/ffmpeg
JAVA_HOME=C:/PROGRA~1/jdk
X264_HOME=/Users/Lubomir/src/x264-snapshot-20090105-2245

TARGET=../../../lib/native/windows/ffmpeg.dll

CC=gcc
CPPFLAGS=-DJNI_IMPLEMENTATION \
         -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32 \
         -I$(FFMPEG_HOME)
#LDFLAGS=-dynamiclib
LDFLAGS=-Wl,--kill-at -shared
LIBS=-L$(FFMPEG_HOME)/libavformat -L$(FFMPEG_HOME)/libavcodec -L$(FFMPEG_HOME)/libavutil \
     -L$(X264_HOME) \
     -lavformat -lavcodec -lavutil -lx264

$(TARGET): net_java_sip_communicator_impl_neomedia_codec_video_FFMPEG.c net_java_sip_communicator_impl_neomedia_codec_video_FFMPEG.h
	$(CC) $(CPPFLAGS) $< $(LDFLAGS) -o $@ $(LIBS)
