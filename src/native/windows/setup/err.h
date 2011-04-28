/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
