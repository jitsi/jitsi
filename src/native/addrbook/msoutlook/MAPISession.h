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

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRBOOK_MSOUTLOOK_MAPISESSION_H_ */
