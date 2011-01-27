MINGW_HOME ?= C:/mingw
TARGET_BASENAME ?= setup
TARGET_DIR ?= ../../../../release/windows/tmp

ifeq ("$(OSTYPE)","cygwin")
    TARGET_DIR = $(shell cygpath --unix $(TARGET_DIR))
endif

CC = $(MINGW_HOME)/bin/gcc
CPPFLAGS = \
	-O2 \
	-Wall -Wreturn-type \
	-DWINVER=0x0502 -D_WIN32_WINNT=0x0502
LDFLAGS = -mwindows
LIBS =
TARGET ?= $(TARGET_DIR)/$(TARGET_BASENAME).exe

MACHINE = $(shell $(CC) -dumpmachine)
WINDRES = $(MINGW_HOME)/bin/windres
ifneq ("x$(MACHINE)","x")
ifeq ($(wildcard $(MINGW_HOME)/bin/$(MACHINE)-windres.*),$(MINGW_HOME)/bin/$(MACHINE)-windres.exe)
    WINDRES = $(MINGW_HOME)/bin/$(MACHINE)-windres
endif
endif

$(TARGET): setup.c $(TARGET_DIR)/setup.res
	$(CC) $(CPPFLAGS) $^ $(LDFLAGS) -o $@ $(LIBS)
	-$(MINGW_HOME)/$(MACHINE)/bin/strip $(TARGET)

$(TARGET_DIR)/setup.res: setup.rc
	$(WINDRES) -I../../../../resources/install/windows $^ -O coff -o $@
