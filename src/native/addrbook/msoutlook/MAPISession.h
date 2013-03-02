/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPISESSION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPISESSION_H_

#include <Mapix.h>

LPMAPISESSION MAPISession_getMapiSession(void);

void MAPISession_setMapiSession(LPMAPISESSION mapiSession);

void MAPISession_initLock();
void MAPISession_lock();
void MAPISession_unlock();
void MAPISession_freeLock();

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPISESSION_H_ */
