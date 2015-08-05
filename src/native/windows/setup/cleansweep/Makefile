#
# Jitsi, the OpenSource Java VoIP and Instant Messaging client.
#
# Copyright @ 2015 Atlassian Pty Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

MINGW_HOME ?= C:/mingw
PRODUCTNAME ?= Jitsi
TARGET_BASENAME ?= cleansweep
TARGET_DIR ?= ../../../../../release/windows/tmp

ifeq ($(wildcard /bin/cygpath.*),/bin/cygpath.exe)
    target.dir := $(shell cygpath --mixed "$(TARGET_DIR)")
    cygwin.target.dir := $(shell cygpath --unix "$(TARGET_DIR)")
else
    target.dir := "$(TARGET_DIR)"
    cygwin.target.dir := "$(TARGET_DIR)"
endif

CC = $(MINGW_HOME)/bin/gcc.exe
CPPFLAGS = \
	-O2 \
	-Wall -Wreturn-type \
	-DWINVER=0x0502 -D_WIN32_WINNT=0x0502 \
	-I$(target.dir)
LDFLAGS = -mwindows
LIBS = -lshell32

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

$(cygwin.target.dir)/$(TARGET_BASENAME).exe: cleansweep.c $(cygwin.target.dir)/config.h $(cygwin.target.dir)/cleansweep.res
	$(CC) $(CPPFLAGS) cleansweep.c $(target.dir)/cleansweep.res $(LDFLAGS) -o $(target.dir)/$(TARGET_BASENAME).exe $(LIBS)
	-$(MINGW_HOME)/$(MACHINE)/bin/strip.exe $(target.dir)/$(TARGET_BASENAME).exe

.PHONY: $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/config.h:
	-rm.exe -f ../../../../../resources/install/windows/config.h
	echo.exe -e '#define PRODUCTNAME "$(PRODUCTNAME)"\n#$(DEFINE_PACKAGECODE)' > $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/cleansweep.res: cleansweep.rc $(cygwin.target.dir)/config.h
	$(WINDRES) -I../../../../../resources/install/windows -I$(target.dir) cleansweep.rc -O coff -o $(target.dir)/cleansweep.res
