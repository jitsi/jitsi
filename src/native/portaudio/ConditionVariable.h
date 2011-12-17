/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_CONDITIONVARIABLE_H_
#define _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_CONDITIONVARIABLE_H_

#include "Mutex.h"

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>

typedef HANDLE ConditionVariable;

static inline void ConditionVariable_free(ConditionVariable *condVar)
{
    if (CloseHandle(*condVar))
        free(condVar);
}

static inline ConditionVariable *ConditionVariable_new(void *attr)
{
    ConditionVariable *condVar = malloc(sizeof(ConditionVariable));

    if (condVar)
    {
        HANDLE event = CreateEvent(NULL, FALSE, FALSE, NULL);

        if (event)
            *condVar = event;
        else
        {
            free(condVar);
            condVar = NULL;
        }
    }
    return condVar;
}

static inline int ConditionVariable_notify(ConditionVariable *condVar)
{
    return SetEvent(*condVar) ? 0 : GetLastError();
}

static inline int ConditionVariable_wait
    (ConditionVariable *condVar, Mutex *mutex)
{
    DWORD waitForSingleObject;

    LeaveCriticalSection(mutex);
    waitForSingleObject = WaitForSingleObject(*condVar, INFINITE);
    EnterCriticalSection(mutex);
    return waitForSingleObject;
}

#else /* #ifdef _WIN32 */
#include <pthread.h>

typedef pthread_cond_t ConditionVariable;

static inline void ConditionVariable_free(ConditionVariable *condVar)
{
    if (!pthread_cond_destroy(condVar))
        free(condVar);
}

static inline ConditionVariable *ConditionVariable_new(void *attr)
{
    ConditionVariable *condVar = malloc(sizeof(ConditionVariable));

    if (condVar && pthread_cond_init(condVar, attr))
    {
        free(condVar);
        condVar = NULL;
    }
    return condVar;
}

static inline int ConditionVariable_notify(ConditionVariable *condVar)
{
    return pthread_cond_signal(condVar);
}

static inline int ConditionVariable_wait
    (ConditionVariable *condVar, Mutex *mutex)
{
    return pthread_cond_wait(condVar, mutex);
}
#endif /* #ifdef _WIN32 */

#endif /* _NET_JAVA_SIP_COMMUNICATOR_IMPL_NEOMEDIA_CONDITIONVARIABLE_H_ */
