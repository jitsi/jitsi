/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MAPISession.h"

static LPMAPISESSION MAPISession_mapiSession = NULL;
static CRITICAL_SECTION MAPISession_mapiSessionCriticalSection;

/**
 * Returns the current mapi session which have been created using the
 * MAPILogonEx function.
 *
 * @return The current mapi session which have been created using the
 * MAPILogonEx function. NULL if no session is currently opened.
 */
LPMAPISESSION MAPISession_getMapiSession(void)
{
    return MAPISession_mapiSession;
}

/**
 * Sets the current mapi session which have been created using the
 * MAPILogonEx function.
 *
 * @param mapiSession The current mapi session which have been created using the
 * MAPILogonEx function.
 */
void MAPISession_setMapiSession(LPMAPISESSION mapiSession)
{
    MAPISession_mapiSession = mapiSession;
}

void MAPISession_initLock()
{
    InitializeCriticalSection(&MAPISession_mapiSessionCriticalSection);
}

void MAPISession_lock()
{
    EnterCriticalSection(&MAPISession_mapiSessionCriticalSection);
}

void MAPISession_unlock()
{
    LeaveCriticalSection(&MAPISession_mapiSessionCriticalSection);
}

void MAPISession_freeLock()
{
    DeleteCriticalSection(&MAPISession_mapiSessionCriticalSection);
}
