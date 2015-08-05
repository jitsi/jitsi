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

#ifndef _ORG_JITSI_WINDOWS_SETUP_ERR_H_
#define _ORG_JITSI_WINDOWS_SETUP_ERR_H_

#ifdef BSPATCH_API_STATIC

#define err(eval, fmt, ...) \
    return(eval)
#define errx(eval, fmt, ...) \
    return(eval)

#else /* #ifdef BSPATCH_API_STATIC */

#define err(eval, fmt, ...) \
    do { \
        if (fmt) { fprintf(stderr, fmt, ##__VA_ARGS__); fprintf(stderr, ": "); } \
        fprintf(stderr, "%s\n", strerror(errno)); \
        exit(eval); \
    } while (0)
#define errx(eval, fmt, ...) \
    do { if (fmt) fprintf(stderr, fmt, ##__VA_ARGS__); exit(eval); } while (0)

#endif /* #ifdef BSPATCH_API_STATIC */

#endif /* #ifndef _ORG_JITSI_WINDOWS_SETUP_ERR_H_ */
