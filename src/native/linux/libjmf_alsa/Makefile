### Makefile --- 

## Author: lorchat@vaio.home.net
## Version: $Id$
## Keywords: 
## X-URL: 

TARGET=libjmf_alsa.so
CFLAGS=-I$(JNI_INCLUDE_PATH) -I$(JNI_INCLUDE_PATH)/linux 
JNI_INCLUDE_PATH=/usr/lib/jvm/java-6-sun-1.6.0.00/include
LIBS=-lasound
OBJS=net_java_sip_communicator_impl_media_protocol_alsa_AlsaStream.o

all:$(TARGET)

clean:
	rm -rf $(TARGET) $(OBJS) *~

install:$(TARGET)
	cp $(TARGET) ../../../../lib/native/linux/

libjmf_alsa.so:$(OBJS)
	$(CC) -shared -o $(TARGET) $(LIBS) $(CFLAGS) $<

### Makefile ends here
