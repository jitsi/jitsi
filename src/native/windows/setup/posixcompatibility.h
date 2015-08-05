/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
