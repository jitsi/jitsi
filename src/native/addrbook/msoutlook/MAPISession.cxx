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
#include "MAPISession.h"
#include <stdio.h>

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
