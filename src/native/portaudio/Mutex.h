/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_MUTEX_H_
#define _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_MUTEX_H_

#include <stdlib.h>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>

typedef CRITICAL_SECTION Mutex;

static inline void Mutex_free(Mutex* mutex)
{
    DeleteCriticalSection(mutex);
    free(mutex);
}

static inline int Mutex_lock(Mutex* mutex)
{
    EnterCriticalSection(mutex);
    return 0;
}

static inline Mutex *Mutex_new(void* attr)
{
    Mutex *mutex = malloc(sizeof(Mutex));

    (void) attr;

    if (mutex)
        InitializeCriticalSection(mutex);
    return mutex;
}

static inline int Mutex_unlock(Mutex* mutex)
{
    LeaveCriticalSection(mutex);
    return 0;
}

#else /* #ifdef _WIN32 */
#include <pthread.h>

typedef pthread_mutex_t Mutex;

static inline void Mutex_free(Mutex* mutex)
{
    if (!pthread_mutex_destroy(mutex))
        free(mutex);
}

static inline int Mutex_lock(Mutex* mutex)
{
    return pthread_mutex_lock(mutex);
}

static inline Mutex *Mutex_new(void* attr)
{
    Mutex *mutex = malloc(sizeof(Mutex));

    if (mutex && pthread_mutex_init(mutex, attr))
    {
        free(mutex);
        mutex = NULL;
    }
    return mutex;
}

static inline int Mutex_unlock(Mutex* mutex)
{
    return pthread_mutex_unlock(mutex);
}
#endif /* #ifdef _WIN32 */

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_PORTAUDIO_MUTEX_H_ */
