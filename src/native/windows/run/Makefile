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
COMPANYNAME ?= jitsi.org
PRODUCTBUILDVERSION ?= 1.0.0.0
PRODUCTBUILDVERSION_COMMA ?= 1,0,0,0
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
	-I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32"
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
	echo.exe -e '#define PRODUCTNAME "$(PRODUCTNAME)"\n#define COMPANYNAME "$(COMPANYNAME)"\n#define PRODUCTBUILDVERSION "$(PRODUCTBUILDVERSION)"\n#define PRODUCTBUILDVERSION_COMMA $(PRODUCTBUILDVERSION_COMMA)\n#define TARGET_BASENAME "$(TARGET_BASENAME)"\n#define TARGET_BASENAME_EXE "$(TARGET_BASENAME).exe"' > $(cygwin.target.dir)/config.h

$(cygwin.target.dir)/run.res: $(cygwin.target.dir)/config.h run.rc
	$(WINDRES) -I../../../../resources/install/windows -I$(target.dir) run.rc -O coff -o $(target.dir)/run.res
