#
# SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
#
# Distributable under LGPL license.
# See terms of license at gnu.org.
#

MINGW_HOME ?= C:/mingw
TARGET_BASENAME ?= cleansweep
TARGET_DIR ?= ../../../../../release/windows/tmp

ifeq ($(wildcard /bin/cygpath.*),/bin/cygpath.exe)
    target.dir := $(shell cygpath --mixed "$(TARGET_DIR)")
    cygwin.target.dir := $(shell cygpath --unix "$(TARGET_DIR)")
else
    target.dir := "$(TARGET_DIR)"
    cygwin.target.dir := "$(TARGET_DIR)"
endif

CC = $(MINGW_HOME)/bin/gcc
CPPFLAGS = \
	-O2 \
	-Wall -Wreturn-type \
	-DPRODUCTNAME='"$(PRODUCTNAME)"' \
	-DWINVER=0x0502 -D_WIN32_WINNT=0x0502
LDFLAGS = -mwindows
LIBS = -lshell32

MACHINE = $(shell $(CC) -dumpmachine)
WINDRES = $(MINGW_HOME)/bin/windres
ifneq ("x$(MACHINE)","x")
ifeq ($(wildcard $(MINGW_HOME)/bin/$(MACHINE)-windres.*),$(MINGW_HOME)/bin/$(MACHINE)-windres.exe)
    WINDRES = $(MINGW_HOME)/bin/$(MACHINE)-windres
endif
endif

$(cygwin.target.dir)/$(TARGET_BASENAME).exe: cleansweep.c $(cygwin.target.dir)/cleansweep.res
	$(CC) $(CPPFLAGS) cleansweep.c "$(target.dir)/cleansweep.res" $(LDFLAGS) -o "$(target.dir)/$(TARGET_BASENAME).exe" $(LIBS)
	-$(MINGW_HOME)/$(MACHINE)/bin/strip "$(target.dir)/$(TARGET_BASENAME).exe"

$(cygwin.target.dir)/cleansweep.res: cleansweep.rc
	$(WINDRES) -I../../../../../resources/install/windows $^ -O coff -o "$(target.dir)/cleansweep.res"
