/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _ORG_JITSI_WINDOWS_SETUP_POSIXCOMPATIBILITY_H_
#define _ORG_JITSI_WINDOWS_SETUP_POSIXCOMPATIBILITY_H_

typedef unsigned char u_char;

#define close(fd) \
    _close(fd)

#define fopen(path, mode) \
    fopen(path, mode "b")

#define fseeko(stream, offset, whence) \
    fseek(stream, offset, whence)

#define lseek(fd, offset, whence) \
    _lseek(fd, offset, whence)

#define open(pathname, flags, mode) \
    _open(pathname, _O_BINARY | flags, mode)

#define read(fd, buf, count) \
    _read(fd, buf, count)

#define write(fd, buf, count) \
    _write(fd, buf, count)

#endif /* #ifndef _ORG_JITSI_WINDOWS_SETUP_POSIXCOMPATIBILITY_H_ */
