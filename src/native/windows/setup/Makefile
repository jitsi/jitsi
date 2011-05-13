#
# Jitsi, the OpenSource Java VoIP and Instant Messaging client.
#
# Distributable under LGPL license.
# See terms of license at gnu.org.
#

BZ2_HOME ?= .
LZMA_HOME ?= .
MINGW_HOME ?= C:/mingw
PRODUCTNAME ?= Jitsi
TARGET_BASENAME ?= setup
TARGET_DIR ?= ../../../../release/windows/tmp

ifeq ($(wildcard /bin/cygpath.*),/bin/cygpath.exe)
    target.dir := $(shell cygpath --mixed "$(TARGET_DIR)")
    cygwin.target.dir := $(shell cygpath --unix "$(TARGET_DIR)")
else
    target.dir := "$(TARGET_DIR)"
    cygwin.target.dir := "$(TARGET_DIR)"
endif

CC = $(MINGW_HOME)/bin/gcc.exe
ARCH = $(shell $(CC) -dumpmachine | sed.exe -e s/x86_64-.*/x64/ -e s/i.86-.*/x86/ -e s/mingw32/x86/)

CPPFLAGS = \
	-O2 \
	-Wall -Wreturn-type \
	-DWINVER=0x0502 -D_WIN32_WINNT=0x0502 \
	-I$(target.dir) \
	-DBSPATCH_API_STATIC -DBZ2_API_STATIC -I. -I$(BZ2_HOME)/include -I$(BZ2_HOME) \
	-DLZMA_API_STATIC -I$(LZMA_HOME)/include -I$(LZMA_HOME)/api \
	-I../run
LDFLAGS = \
	-mwindows \
	-L$(BZ2_HOME)/lib/$(ARCH) -L$(BZ2_HOME) \
	-L$(LZMA_HOME)/lib/$(ARCH) -L$(LZMA_HOME)/.libs
LIBS = -ladvapi32 -lbz2 -llzma -lole32 -lshell32

MACHINE = $(shell $(CC) -dumpmachine)
WINDRES = $(MINGW_HOME)/bin/windres.exe
ifneq ("x$(MACHINE)","x")
ifeq ($(wildcard $(MINGW_HOME)/bin/$(MACHINE)-windres.*),$(MINGW_HOME)/bin/$(MACHINE)-windres.exe)
    WINDRES = $(MINGW_HOME)/bin/$(MACHINE)-windres.exe
endif
endif

ifdef PACKAGECODE
    DEFINE_PACKAGECODE = define PACKAGECODE "$(strip $(PACKAGECODE))"
else
    DEFINE_PACKAGECODE = undef PACKAGECODE
endif
ifdef PACKAGESIZE
    DEFINE_PACKAGESIZE = define PACKAGESIZE $(strip $(PACKAGESIZE))
else
    DEFINE_PACKAGESIZE = undef PACKAGESIZE
endif

$(cygwin.target.dir)/$(TARGET_BASENAME).exe: bspatch.c $(cygwin.target.dir)/config.h lasterror.c ../run/registry.c setup.c $(cygwin.target.dir)/setup.res
	$(CC) $(CPPFLAGS) bspatch.c lasterror.c ../run/registry.c setup.c $(target.dir)/setup.res $(LDFLAGS) -o $(target.dir)/$(TARGET_BASENAME).exe $(LIBS)
	-$(MINGW_HOME)/$(MACHINE)/bin/strip.exe $(target.dir)/$(TARGET_BASENAME).exe

.PHONY: $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/config.h:
	-rm.exe -f ../../../../resources/install/windows/config.h
	echo.exe -e '#define PRODUCTNAME "$(PRODUCTNAME)"\n#$(DEFINE_PACKAGECODE)\n#$(DEFINE_PACKAGESIZE)' > $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/setup.res: $(cygwin.target.dir)/config.h setup.rc
	$(WINDRES) -I../../../../resources/install/windows -I$(target.dir) setup.rc -O coff -o $(target.dir)/setup.res
