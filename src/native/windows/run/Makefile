#
# Jitsi, the OpenSource Java VoIP and Instant Messaging client.
#
# Distributable under LGPL license.
# See terms of license at gnu.org.
#

MINGW_HOME ?= C:/mingw
PRODUCTNAME ?= Jitsi
TARGET_BASENAME ?= run
TARGET_DIR ?= ../../../../release/windows/tmp

ifeq ($(wildcard /bin/cygpath.*),/bin/cygpath.exe)
    target.dir := $(shell cygpath --mixed "$(TARGET_DIR)")
    cygwin.target.dir := $(shell cygpath --unix "$(TARGET_DIR)")
else
    target.dir := "$(TARGET_DIR)"
    cygwin.target.dir := "$(TARGET_DIR)"
endif

CC = $(MINGW_HOME)/bin/gcc.exe
CPPFLAGS := $(CPPFLAGS) \
	-O2 \
	-Wall -Wreturn-type \
	-DPSAPI_VERSION=1 \
	-DWINVER=0x0502 -D_WIN32_WINNT=0x0502 \
	-I$(target.dir) \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32
LDFLAGS = -mwindows
LIBS = -ladvapi32 -lpsapi

MACHINE = $(shell $(CC) -dumpmachine)
WINDRES = $(MINGW_HOME)/bin/windres.exe
ifneq ("x$(MACHINE)","x")
ifeq ($(wildcard $(MINGW_HOME)/bin/$(MACHINE)-windres.*),$(MINGW_HOME)/bin/$(MACHINE)-windres.exe)
    WINDRES = $(MINGW_HOME)/bin/$(MACHINE)-windres.exe
endif
endif

$(cygwin.target.dir)/$(TARGET_BASENAME).exe: $(cygwin.target.dir)/config.h registry.c run.c $(cygwin.target.dir)/run.res ../setup/nls.c
	$(CC) $(CPPFLAGS) registry.c run.c $(target.dir)/run.res ../setup/nls.c $(LDFLAGS) -o $(target.dir)/$(TARGET_BASENAME).exe $(LIBS)
	-$(MINGW_HOME)/$(MACHINE)/bin/strip.exe $(target.dir)/$(TARGET_BASENAME).exe

.PHONY: $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/config.h:
	-rm.exe -f ../../../../resources/install/windows/config.h
	echo.exe -e '#define PRODUCTNAME "$(PRODUCTNAME)"' > $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/run.res: $(cygwin.target.dir)/config.h run.rc
	$(WINDRES) -I../../../../resources/install/windows -I$(target.dir) run.rc -O coff -o $(target.dir)/run.res
