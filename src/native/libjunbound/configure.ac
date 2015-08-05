# Prelude.
AC_INIT([junbound], [1.0], [dev@jitsi.java.net])
AM_INIT_AUTOMAKE([foreign -Wall -Werror])
AC_CONFIG_MACRO_DIR([m4])
LT_INIT

# Checks for programs.
AC_PROG_CXX
# Checks for libraries.

AC_CHECK_LIB([unbound], [ub_ctx_create], [], [AC_MSG_ERROR([libunbound not found (is libunbound-dev installed?)])])

# Checks for header files.
AC_MSG_CHECKING(for java include file jni.h)
AC_ARG_WITH(javaincl, [  --with-javaincl=path    Set location of Java include directory], [JAVAINCDIR="$withval"], [JAVAINCDIR=])

if test -z "$JAVAINCDIR" ; then
  JAVAINCDIR="/usr/j2sdk*/include /usr/local/j2sdk*/include /usr/jdk*/include /usr/local/jdk*/include /opt/j2sdk*/include /opt/jdk*/include /usr/java/include /usr/java/j2sdk*/include /usr/java/jdk*/include /usr/local/java/include /opt/java/include /usr/include/java /usr/local/include/java /usr/lib/java/include /usr/lib/jvm/java*/include /usr/include/kaffe /usr/local/include/kaffe /usr/include"

  # Add in default installation directory on Windows for Cygwin
  case $host in
  *-*-cygwin* | *-*-mingw*) JAVAINCDIR="c:/Program*Files/Java/jdk*/include d:/Program*Files/Java/jdk*/include c:/j2sdk*/include d:/j2sdk*/include c:/jdk*/include d:/jdk*/include $JAVAINCDIR";;
  *-*-darwin*) JAVAINCDIR="/System/Library/Frameworks/JavaVM.framework/Headers $JAVAINCDIR";;
  *);;
  esac
fi

if test -z "$JAVA_HOME" ; then
  JAVA_HOME=":" # Something completely invalid
fi
VS_JNI_INCLUDE=""
for d in "$JAVA_HOME/include" $JAVAINCDIR ; do
  if test -r "$d/jni.h" ; then
    AC_MSG_RESULT($d)
    JAVAINCDIR=$d
    VS_JNI_INCLUDE=-I\"$d\" #" quote here to fix nasty formatting bug in VIM
    break
  fi
done

if test "$VS_JNI_INCLUDE" = "" ; then
  AC_MSG_RESULT(not found)
  AC_MSG_FAILURE([Could not find required Java header file jni.h. 
                  Try defining JAVA_HOME to point to where you've
                  installed the JAVA JDK])
else
  # now look for <arch>/jni_md.h
  AC_MSG_CHECKING(for java include file jni_md.h)
  JAVAMDDIR=`find "$JAVAINCDIR" -follow -name jni_md.h -print`
  if test "$JAVAMDDIR" = "" ; then
    AC_MSG_RESULT(not found)
    AC_MSG_FAILURE([Could not find required Java header file jni_md.h. 
                  Try defining JAVA_HOME to point to where you've
                  installed the JAVA JDK])
  else
    JAVAMDDIR=`dirname "$JAVAMDDIR" | tail -1`
    VS_JNI_INCLUDE="${VS_JNI_INCLUDE} -I\"$JAVAMDDIR\""
    AC_MSG_RESULT($JAVAMDDIR)
  fi
fi

AC_SUBST([VS_JNI_INCLUDE])
#AC_CHECK_HEADERS([jni.h], [], [AC_MSG_ERROR([JNI headers not found])])
# Checks for typedefs, structures, and compiler characteristics.
# Checks for library functions.
# Output files.
# AC_CONFIG_HEADERS([config.h])
AC_CONFIG_FILES([Makefile src/Makefile])
AC_OUTPUT