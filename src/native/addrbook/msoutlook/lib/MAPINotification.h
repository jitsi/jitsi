/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _mapi_notification_h
#define _mapi_notification_h

#ifdef __cplusplus
extern "C" {
#endif

#include <mapidefs.h>
#include <mapix.h>

LONG STDAPICALLTYPE onNotify(
        LPVOID lpvContext,
        ULONG cNotifications,
        LPNOTIFICATION lpNotifications);

ULONG registerNotifyMessageDataBase(
        LPMDB iUnknown);

#ifdef __cplusplus
}
#endif

#endif
